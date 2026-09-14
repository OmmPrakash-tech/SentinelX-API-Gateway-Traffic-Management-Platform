package com.example.backend;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class Registry {
    final Db db;final Set<String> allowed;final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).followRedirects(HttpClient.Redirect.NEVER).build();
    final ConcurrentHashMap<String,Health> health=new ConcurrentHashMap<>();
    final ConcurrentHashMap<String,TrafficPrimitives.Balancer> balancers=new ConcurrentHashMap<>();
    final ConcurrentHashMap<String,TrafficPrimitives.Circuit> circuits=new ConcurrentHashMap<>();
    public Registry(Db db,@Value("${sentinel.allowed-hosts}")String allowed){this.db=db;this.allowed=Set.of(allowed.split(","));}
    URI validate(String url){try{URI uri=URI.create(url);if(!Set.of("http","https").contains(uri.getScheme())||!allowed.contains(uri.getHost())||uri.getUserInfo()!=null||uri.getRawQuery()!=null||uri.getFragment()!=null||!(uri.getPath().isEmpty()||uri.getPath().equals("/")))throw new IllegalArgumentException();return uri;}catch(Exception e){throw new ApiError(400,"INVALID_UPSTREAM","Use an HTTP origin on the configured upstream host allowlist");}}
    static class Health {
        volatile boolean healthy;volatile long lastCheck;volatile double latency;volatile int failures;volatile int successes;volatile long lastFailure;
        Map<String,Object> snapshot(){return Map.of("status",healthy?"HEALTHY":lastCheck==0?"UNKNOWN":"UNHEALTHY","lastCheck",lastCheck,"latencyMs",latency,"failures",failures,"successes",successes,"lastFailure",lastFailure);}
    }
    @Scheduled(fixedDelayString="${sentinel.health-interval}")
    public void check(){var instances=db.rows("SELECT i.* FROM service_instances i JOIN services s ON s.id=i.service_id WHERE i.enabled=TRUE AND s.enabled=TRUE");try(var executor=Executors.newVirtualThreadPerTaskExecutor()){for(var row:instances)executor.submit(()->probe(row));}}
    void probe(Map<String,Object> row){String id=Db.str(row,"id");Health h=health.computeIfAbsent(id,k->new Health());long start=System.nanoTime();boolean ok=false;
        try{String base=validate(Db.str(row,"base_url")).toString().replaceAll("/$","");var response=client.send(HttpRequest.newBuilder(URI.create(base+Db.str(row,"health_path"))).timeout(Duration.ofSeconds(1)).GET().build(),HttpResponse.BodyHandlers.discarding());ok=response.statusCode()==200;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();}
        synchronized(h){h.lastCheck=System.currentTimeMillis();h.latency=(System.nanoTime()-start)/1e6;if(ok){h.successes++;h.failures=0;h.healthy=true;}else{h.failures++;h.successes=0;h.lastFailure=h.lastCheck;h.healthy=false;}}
    }
    List<Map<String,Object>> instances(){var rows=db.rows("SELECT i.*,s.name AS service_name FROM service_instances i JOIN services s ON i.service_id=s.id ORDER BY s.name,i.id");for(var row:rows){String id=Db.str(row,"id"),sid=Db.str(row,"service_id");row.putAll(health.computeIfAbsent(id,k->new Health()).snapshot());row.put("activeRequests",balancers.computeIfAbsent(sid,k->new TrafficPrimitives.Balancer()).active(id));}return rows;}
    List<Map<String,Object>> healthy(String service){long now=System.currentTimeMillis();return db.rows("SELECT * FROM service_instances WHERE service_id=? AND enabled=TRUE ORDER BY id",service).stream().filter(r->{Health h=health.get(Db.str(r,"id"));return h!=null&&h.healthy&&now-h.lastCheck<15000;}).toList();}
    Map<String,Object> route(String path,String method){return db.rows("SELECT r.*,s.strategy,s.name AS service_name FROM routes r JOIN services s ON s.id=r.service_id WHERE r.enabled=TRUE AND s.enabled=TRUE ORDER BY LENGTH(r.path_prefix) DESC").stream().filter(r->TrafficPrimitives.matches(Db.str(r,"path_prefix"),path)&&Arrays.asList(Db.str(r,"methods").split(",")).contains(method)).findFirst().orElseThrow(()->new ApiError(404,"NO_ROUTE","No enabled route matches this path and method"));}
}
