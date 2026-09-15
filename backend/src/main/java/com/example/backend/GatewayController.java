package com.example.backend;

import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;

@RestController
public class GatewayController {
    final Db db;final Registry registry;final KeyController keys;final ObjectMapper json;
    final TrafficPrimitives.Limiter limiter=new TrafficPrimitives.Limiter();
    record ClientConfig(int timeout,HttpClient client){}
    final ConcurrentHashMap<String,ClientConfig> clients=new ConcurrentHashMap<>();
    HttpClient clientFor(Map<String,Object> route){String id=Db.str(route,"id");int timeout=Db.num(route,"connect_timeout_ms");return clients.compute(id,(k,old)->{if(old!=null&&old.timeout==timeout)return old;if(old!=null)old.client.shutdown();return new ClientConfig(timeout,HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeout)).followRedirects(HttpClient.Redirect.NEVER).build());}).client;}
    static final Set<String> SAFE_REQUEST_HEADERS=Set.of("accept","content-type","accept-language","if-none-match","if-modified-since");
    static final Set<String> SAFE_RESPONSE_HEADERS=Set.of("content-type","etag","last-modified","cache-control","content-language","x-instance-id");
    public GatewayController(Db db,Registry registry,KeyController keys,ObjectMapper json){this.db=db;this.registry=registry;this.keys=keys;this.json=json;}
    @GetMapping("/api/me/quotas")
    Object quotas(HttpServletRequest req){
        String user=AccountController.user(req);var result=new ArrayList<Map<String,Object>>();
        for(var policy:db.rows("SELECT * FROM rate_limit_policies WHERE enabled=TRUE AND scope IN ('USER','KEY')")){
            String scope=Db.str(policy,"scope");var subjects=scope.equals("KEY")?db.rows("SELECT id,name,tier FROM api_keys WHERE user_id=? AND active=TRUE AND revoked=FALSE AND expires_at>?",user,Db.now()):List.of(Map.<String,Object>of("id",user,"name","Account","tier","STANDARD"));
            for(var subject:subjects){int multiplier=scope.equals("KEY")&&Db.str(subject,"tier").equals("ELEVATED")?2:1;int cap=Db.num(policy,"capacity")*multiplier;double refill=((Number)policy.get("refill_per_second")).doubleValue()*multiplier;
                var quota=limiter.peek(Db.str(policy,"id")+":"+cap+":"+refill+":"+Db.str(subject,"id"),cap,refill);
                result.add(Map.of("scope",scope,"name",subject.get("name"),"capacity",cap,"remaining",quota.remaining(),"refillPerSecond",refill));
            }
        }return result;
    }
    @RequestMapping(value="/gateway/**",method={RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT,RequestMethod.PATCH,RequestMethod.DELETE,RequestMethod.HEAD,RequestMethod.OPTIONS})
    public void proxy(HttpServletRequest req,HttpServletResponse res)throws IOException {
        long start=System.nanoTime();String requestId=String.valueOf(req.getAttribute("requestId")),path=req.getRequestURI().substring("/gateway".length());
        String user=null,key=null,service=null,instance=null;int status=500,retries=0;
        try{
            String rawKey=req.getHeader("X-API-Key");
            if(rawKey!=null){var owner=keys.validate(rawKey);user=Db.str(owner,"id");key=Db.str(owner,"key_id");}
            else if(req.getUserPrincipal()!=null){user=req.getUserPrincipal().getName();if(!Db.bool(db.one("SELECT api_enabled FROM users WHERE id=?",user),"api_enabled"))throw new ApiError(403,"API_DISABLED","API access is disabled");}
            else throw new ApiError(401,"API_AUTH_REQUIRED","Supply X-API-Key or a valid bearer session");
            if(Db.str(db.one("SELECT role FROM users WHERE id=?",user),"role").equals("USER"))throw new ApiError(403,"DEVELOPER_ACCESS_REQUIRED","A developer or operations role is required for gateway access");
            if(path.length()>500||path.contains("%")||path.contains("..")||path.contains("//")||path.contains("\\"))throw new ApiError(400,"INVALID_PATH","Use a canonical gateway path");
            var route=registry.route(path,req.getMethod());service=Db.str(route,"service_id");
            double tier=1;if(key!=null&&Db.str(db.one("SELECT tier FROM api_keys WHERE id=?",key),"tier").equals("ELEVATED"))tier=2;
            for(var policy:db.rows("SELECT * FROM rate_limit_policies WHERE enabled=TRUE ORDER BY scope")){
                String scope=Db.str(policy,"scope");String subject=switch(scope){case "IP"->req.getRemoteAddr();case "USER"->user;case "KEY"->key;case "ROUTE"->Db.str(route,"id");default->"all";};if(subject==null)continue;
                int cap=Db.num(policy,"capacity");double refill=((Number)policy.get("refill_per_second")).doubleValue();if(scope.equals("KEY")){cap=(int)(cap*tier);refill*=tier;}
                var quota=limiter.take(Db.str(policy,"id")+":"+cap+":"+refill+":"+subject,cap,refill);
                res.setHeader("X-RateLimit-"+scope+"-Limit",String.valueOf(cap));res.setHeader("X-RateLimit-"+scope+"-Remaining",String.valueOf(quota.remaining()));
                if(!quota.allowed()){res.setHeader("Retry-After",String.valueOf(quota.retryAfter()));throw new ApiError(429,"RATE_LIMIT_EXCEEDED","Request rate limit exceeded ("+scope+")");}
            }
            byte[] body=req.getInputStream().readNBytes(1_048_577);if(body.length>1_048_576)throw new ApiError(413,"PAYLOAD_TOO_LARGE","Maximum request body is 1 MiB");
            var balancer=registry.balancers.computeIfAbsent(service,k->new TrafficPrimitives.Balancer());
            long deadline=System.nanoTime()+Db.num(route,"timeout_ms")*1_000_000L;
            for(int attempt=0;attempt<=Db.num(route,"retries");attempt++){
                String routeId=Db.str(route,"id");
                var candidates=registry.healthy(service).stream().filter(r->{var c=registry.circuits.get(routeId+":"+Db.str(r,"id"));return c==null||c.available(System.currentTimeMillis(),Db.num(route,"recovery_ms"));}).toList();var ids=candidates.stream().map(r->Db.str(r,"id")).toList();
                instance=balancer.acquire(ids,Db.str(route,"strategy"));
                var circuit=registry.circuits.computeIfAbsent(Db.str(route,"id")+":"+instance,k->new TrafficPrimitives.Circuit());
                long permit=-1;boolean success=false;
                try{
                    permit=circuit.acquire(System.currentTimeMillis(),Db.num(route,"recovery_ms"));if(permit<0)throw new ApiError(503,"CIRCUIT_OPEN","Upstream circuit is open");
                    long remaining=deadline-System.nanoTime();if(remaining<=0)throw new ApiError(504,"UPSTREAM_TIMEOUT","Overall request deadline exceeded");
                    String selected=instance;var target=candidates.stream().filter(r->Db.str(r,"id").equals(selected)).findFirst().orElseThrow();
                    String query=req.getQueryString();String origin=registry.validate(Db.str(target,"base_url")).toString().replaceAll("/$","");
                    var builder=HttpRequest.newBuilder(URI.create(origin+path+(query==null?"":"?"+query))).timeout(Duration.ofNanos(remaining)).header("X-Request-ID",requestId);
                    for(String name:SAFE_REQUEST_HEADERS){String value=req.getHeader(name);if(value!=null)builder.header(name,value);}
                    builder.method(req.getMethod(),body.length==0?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body));
                    var pending=clientFor(route).sendAsync(builder.build(),info->new BoundedResponse(8_388_608,Db.num(route,"read_timeout_ms")));
                    HttpResponse<byte[]> response;
                    try{response=pending.get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);}
                    catch(TimeoutException e){pending.cancel(true);throw new HttpTimeoutException("Overall deadline exceeded");}
                    catch(ExecutionException e){if(e.getCause() instanceof HttpTimeoutException||e.getCause() instanceof TimeoutException)throw new HttpTimeoutException("Upstream timeout");throw new IOException("Upstream failure",e.getCause());}
                    status=response.statusCode();success=status<500;
                    if(response.body().length>8_388_608)throw new ApiError(502,"RESPONSE_TOO_LARGE","Upstream response exceeds the 8 MiB gateway limit");
                    if(attempt<Db.num(route,"retries")&&TrafficPrimitives.retryable(req.getMethod(),status)){retries++;}
                    else{res.setStatus(status);response.headers().map().forEach((name,values)->{if(SAFE_RESPONSE_HEADERS.contains(name.toLowerCase(Locale.ROOT)))res.setHeader(name,String.join(",",values));});res.setHeader("X-SentinelX-Instance",instance);res.setHeader("X-SentinelX-Retries",String.valueOf(retries));res.getOutputStream().write(response.body());return;}
                }catch(HttpTimeoutException e){status=504;if(attempt>=Db.num(route,"retries")||!TrafficPrimitives.retryable(req.getMethod(),status))throw new ApiError(504,"UPSTREAM_TIMEOUT","Upstream request timed out");retries++;}
                catch(InterruptedException e){Thread.currentThread().interrupt();throw new ApiError(503,"INTERRUPTED","Gateway request interrupted");}
                catch(IOException e){status=502;if(attempt>=Db.num(route,"retries")||!TrafficPrimitives.retryable(req.getMethod(),status))throw new ApiError(502,"UPSTREAM_UNAVAILABLE","Upstream connection failed");retries++;}
                finally{balancer.release(instance);if(permit>=0)circuit.complete(permit,success,Db.num(route,"failure_threshold"),System.currentTimeMillis());}
                long backoff=Math.min(Db.num(route,"backoff_ms")*(1L<<attempt),1000);if(System.nanoTime()+backoff*1_000_000>=deadline)throw new ApiError(504,"UPSTREAM_TIMEOUT","Overall request deadline exceeded");
                try{Thread.sleep(backoff);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new ApiError(503,"INTERRUPTED","Gateway request interrupted");}
            }
        }catch(ApiError e){status=e.status;res.setStatus(status);res.setContentType("application/json");json.writeValue(res.getOutputStream(),Errors.body(status,e.code,e.getMessage(),requestId));}
        finally{
            double latency=(System.nanoTime()-start)/1e6;
            db.update("INSERT INTO request_logs VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",Db.id(),requestId,user,key,req.getMethod(),path.length()>500?path.substring(0,500):path,service,instance,status,latency,retries,Db.now());
            LoggerFactory.getLogger(GatewayController.class).atInfo().addKeyValue("requestId",requestId).addKeyValue("userId",user).addKeyValue("apiConsumerId",key).addKeyValue("method",req.getMethod()).addKeyValue("path",path.length()>500?"[oversized]":path).addKeyValue("status",status).addKeyValue("latencyMs",latency).addKeyValue("retries",retries).log("gateway_request");
        }
    }
}

