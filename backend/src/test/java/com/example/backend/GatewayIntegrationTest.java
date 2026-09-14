package com.example.backend;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE}","sentinel.seed=false","sentinel.health-interval=60000"})
class GatewayIntegrationTest {
    @LocalServerPort int port;@Autowired Db db;@Autowired AuthService auth;@Autowired KeyController keys;@Autowired Registry registry;@Autowired ObjectMapper json;
    final HttpClient client=HttpClient.newHttpClient();HttpServer a,b;final AtomicInteger failures=new AtomicInteger(),calls=new AtomicInteger();final AtomicBoolean down=new AtomicBoolean();
    String user,other,admin,key,keyId,service,route,ia,ib;
    @BeforeEach void setup()throws Exception{
        String suffix=Db.id();user=Db.str(auth.register("Alpha",suffix+"@example.test","Local-password-123","Local-password-123"),"id");other=Db.str(auth.register("Beta",suffix+"b@example.test","Local-password-123","Local-password-123"),"id");
        db.update("UPDATE users SET role='ADMIN' WHERE id=?",user);admin=(String)auth.login(suffix+"@example.test","Local-password-123").get("token");
        var credential=keys.createKey(user,"Integration",1);key=(String)credential.get("secret");keyId=(String)credential.get("id");
        a=server("A");b=server("B");service=Db.id();ia=Db.id();ib=Db.id();route=Db.id();db.update("INSERT INTO services VALUES(?,?,?,?,?)",service,"test-"+suffix,true,"ROUND_ROBIN",Db.now());
        db.update("INSERT INTO service_instances VALUES(?,?,?,?,?,?)",ia,service,"http://localhost:"+a.getAddress().getPort(),"/health",true,Db.now());db.update("INSERT INTO service_instances VALUES(?,?,?,?,?,?)",ib,service,"http://localhost:"+b.getAddress().getPort(),"/health",true,Db.now());
        db.update("INSERT INTO routes(id,path_prefix,methods,service_id,enabled,timeout_ms,retries,backoff_ms,failure_threshold,recovery_ms,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",route,"/test-"+suffix,"GET,POST",service,true,2000,1,0,2,100,Db.now());registry.check();
    }
    HttpServer server(String name)throws Exception{var s=HttpServer.create(new InetSocketAddress("localhost",0),0);s.createContext("/",e->{boolean health=e.getRequestURI().getPath().equals("/health");int status=health?(down.get()?503:200):(failures.getAndUpdate(v->Math.max(0,v-1))>0?503:200);if(!health)calls.incrementAndGet();var data=json.writeValueAsBytes(Map.of("instance",name,"requestId",Objects.toString(e.getRequestHeaders().getFirst("X-Request-ID"),""),"authorizationForwarded",e.getRequestHeaders().containsKey("Authorization"),"keyForwarded",e.getRequestHeaders().containsKey("X-API-Key")));e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(status,data.length);e.getResponseBody().write(data);e.close();});s.start();return s;}
    @AfterEach void stop(){if(a!=null)a.stop(0);if(b!=null)b.stop(0);db.update("DELETE FROM rate_limit_policies");db.update("UPDATE routes SET enabled=FALSE WHERE id=?",route);db.update("UPDATE services SET enabled=FALSE WHERE id=?",service);}
    String path(){return "/gateway"+Db.str(db.one("SELECT path_prefix FROM routes WHERE id=?",route),"path_prefix");}
    HttpResponse<String> request(String method,String path,String apiKey,String bearer,Object body)throws Exception{
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("X-Request-ID","integration-check").header("Content-Type","application/json");if(apiKey!=null)builder.header("X-API-Key",apiKey);if(bearer!=null)builder.header("Authorization","Bearer "+bearer);
        return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void proxyRoundRobinHealthRecoveryAndCorrelation()throws Exception{
        var first=request("GET",path(),key,null,null);var second=request("GET",path(),key,null,null);assertEquals(200,first.statusCode(),first.body());assertEquals(200,second.statusCode());assertNotEquals(json.readValue(first.body(),Map.class).get("instance"),json.readValue(second.body(),Map.class).get("instance"));assertEquals("integration-check",first.headers().firstValue("X-Request-ID").orElseThrow());assertTrue(first.body().contains("integration-check"));assertTrue(first.body().contains("\"keyForwarded\":false"));
        a.stop(0);registry.check();for(int i=0;i<4;i++){var res=request("GET",path(),key,null,null);assertEquals(200,res.statusCode());assertEquals("B",json.readValue(res.body(),Map.class).get("instance"));}
        a=server("A");db.update("UPDATE service_instances SET base_url=? WHERE id=?","http://localhost:"+a.getAddress().getPort(),ia);registry.check();assertEquals(2,registry.healthy(service).size());
        db.update("UPDATE services SET strategy='LEAST_CONNECTIONS' WHERE id=?",service);assertEquals(200,request("GET",path(),key,null,null).statusCode());
        assertEquals(7,Db.num(db.one("SELECT COUNT(*) AS n FROM request_logs WHERE user_id=?",user),"n"));
    }
    @Test void retrySafeReadsButNotWritesAndCircuitRecovery()throws Exception{
        db.update("UPDATE service_instances SET enabled=FALSE WHERE id=?",ib);failures.set(1);var retried=request("GET",path(),key,null,null);assertEquals(200,retried.statusCode());assertEquals("1",retried.headers().firstValue("X-SentinelX-Retries").orElseThrow());assertEquals(2,calls.get());
        failures.set(1);var write=request("POST",path(),key,null,Map.of("name","x"));assertEquals(503,write.statusCode());assertEquals(3,calls.get());
        failures.set(100);request("POST",path(),key,null,Map.of());int before=calls.get();assertEquals(503,request("GET",path(),key,null,null).statusCode());assertEquals(before,calls.get());
        failures.set(0);Thread.sleep(150);assertEquals(200,request("GET",path(),key,null,null).statusCode());assertEquals("CLOSED",registry.circuits.get(route+":"+ia).snapshot().get("state"));
    }
    @Test void rateLimitAndRevokedKey()throws Exception{
        db.update("INSERT INTO rate_limit_policies VALUES(?,?,?,?,?)",Db.id(),"KEY",2,.001,true);assertEquals(200,request("GET",path(),key,null,null).statusCode());assertEquals(200,request("GET",path(),key,null,null).statusCode());var limited=request("GET",path(),key,null,null);assertEquals(429,limited.statusCode());assertTrue(limited.headers().firstValue("Retry-After").isPresent());
        db.update("UPDATE api_keys SET revoked=TRUE WHERE id=?",keyId);assertEquals(401,request("GET",path(),key,null,null).statusCode());assertEquals(401,request("GET",path(),null,null,null).statusCode());
    }
    @Test void ownershipAdminSecurityAndForcedReset()throws Exception{
        String email=Db.str(auth.profile(other),"email"),otherToken=(String)auth.login(email,"Local-password-123").get("token");
        assertEquals(403,request("GET","/api/admin/users",null,otherToken,null).statusCode());assertEquals(404,request("DELETE","/api/me/keys/"+keyId,null,otherToken,null).statusCode());
        request("GET",path(),key,null,null);var stats=request("GET","/api/me/metrics",null,otherToken,null);assertEquals(0,((Number)json.readValue(stats.body(),Map.class).get("total")).intValue());
        var users=request("GET","/api/admin/users",null,admin,null);assertEquals(200,users.statusCode());assertFalse(users.body().contains("password_hash"));assertFalse(users.body().contains("$2"));
        assertEquals(200,request("POST","/api/admin/users/"+other+"/force-reset",null,admin,null).statusCode());assertThrows(ApiError.class,()->auth.login(email,"Local-password-123"));assertEquals(401,request("GET","/api/me",null,otherToken,null).statusCode());
    }
    @Test void managementValidationAndOperatorBoundary()throws Exception{
        var rejected=request("POST","/api/admin/instances",null,admin,Map.of("serviceId",service,"baseUrl","http://169.254.169.254","healthPath","/health","enabled",true));assertEquals(400,rejected.statusCode());
        var policy=request("POST","/api/admin/rate-limits",null,admin,Map.of("scope","USER","capacity",10,"refillPerSecond",1,"enabled",true));assertEquals(200,policy.statusCode(),policy.body());
        db.update("UPDATE users SET role='OPERATOR' WHERE id=?",user);assertEquals(200,request("GET","/api/ops/services",null,admin,null).statusCode());assertEquals(403,request("GET","/api/admin/users",null,admin,null).statusCode());
    }
    @Test void everyRateScopeIsEnforced()throws Exception{
        for(String scope:List.of("GLOBAL","IP","USER","KEY","ROUTE")){db.update("DELETE FROM rate_limit_policies");db.update("INSERT INTO rate_limit_policies VALUES(?,?,?,?,?)",Db.id(),scope,1,.001,true);assertEquals(200,request("GET",path(),key,null,null).statusCode(),scope);assertEquals(429,request("GET",path(),key,null,null).statusCode(),scope);}
    }
    @Test void expiredKeyAndDisabledApiAccess()throws Exception{
        db.update("UPDATE api_keys SET expires_at=? WHERE id=?",java.sql.Timestamp.valueOf("2000-01-01 00:00:00"),keyId);assertEquals(401,request("GET",path(),key,null,null).statusCode());
        db.update("UPDATE users SET api_enabled=FALSE WHERE id=?",user);assertEquals(403,request("GET",path(),null,admin,null).statusCode());
    }
    @Test void basicUserDiffersFromDeveloperAndOperator()throws Exception{
        db.update("UPDATE users SET role='USER' WHERE id=?",user);assertEquals(200,request("GET","/api/me",null,admin,null).statusCode());assertEquals(403,request("GET","/api/ops/services",null,admin,null).statusCode());assertEquals(403,request("POST","/api/me/keys",null,admin,Map.of("name","Forbidden","days",1)).statusCode());assertEquals(403,request("GET",path(),key,null,null).statusCode());
    }
    @Test void dynamicRouteCreationUpdateAndRemoval()throws Exception{
        var body=new LinkedHashMap<String,Object>();body.put("pathPrefix","/dynamic-"+Db.id());body.put("methods","GET");body.put("serviceId",service);body.put("enabled",true);body.put("timeoutMs",2000);body.put("connectTimeoutMs",500);body.put("readTimeoutMs",1000);body.put("retries",0);body.put("backoffMs",0);body.put("failureThreshold",3);body.put("recoveryMs",500);
        var created=request("POST","/api/admin/routes",null,admin,body);assertEquals(200,created.statusCode(),created.body());String id=(String)json.readValue(created.body(),Map.class).get("id");
        assertEquals(200,request("GET","/gateway"+body.get("pathPrefix"),key,null,null).statusCode());body.put("enabled",false);assertEquals(200,request("PUT","/api/admin/routes/"+id,null,admin,body).statusCode());assertEquals(404,request("GET","/gateway"+body.get("pathPrefix"),key,null,null).statusCode());assertEquals(200,request("DELETE","/api/admin/routes/"+id,null,admin,null).statusCode());
    }
}
