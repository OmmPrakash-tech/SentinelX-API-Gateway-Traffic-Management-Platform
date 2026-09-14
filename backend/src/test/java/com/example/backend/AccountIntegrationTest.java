package com.example.backend;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:account;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE}","sentinel.reset-directory=./target/test-mailbox","sentinel.seed=false"})
class AccountIntegrationTest {
    @LocalServerPort int port;@Autowired Db db;@Autowired AuthService auth;@Autowired ObjectMapper json;
    final HttpClient client=HttpClient.newHttpClient();
    HttpResponse<String> request(String method,String path,Object body,String token)throws Exception{
        var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");if(token!=null)b.header("Authorization","Bearer "+token);
        return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    Map<?,?> parse(HttpResponse<String> r){return json.readValue(r.body(),Map.class);}
    @Test void fullAccountLifecycleAndAuthorization()throws Exception{
        String email="one@example.test",p="Local-password-123",next="Changed-password-456";
        var reg=request("POST","/api/auth/register",Map.of("name","One","email",email,"password",p,"confirmPassword",p),null);assertEquals(200,reg.statusCode(),reg.body());assertFalse(reg.body().contains("password"));
        String id=(String)parse(reg).get("id");String hash=Db.str(db.one("SELECT password_hash FROM users WHERE id=?",id),"password_hash");assertTrue(hash.startsWith("$2"));assertNotEquals(p,hash);
        var bad=request("POST","/api/auth/login",Map.of("email",email,"password","wrong"),null);assertEquals(401,bad.statusCode());
        var login=request("POST","/api/auth/login",Map.of("email",email,"password",p),null);assertEquals(200,login.statusCode(),login.body());String token=(String)parse(login).get("token");
        assertEquals(200,request("GET","/api/me",null,token).statusCode());assertEquals(403,request("GET","/api/admin/users",null,token).statusCode());
        assertEquals(200,request("POST","/api/auth/logout",null,token).statusCode());assertEquals(401,request("GET","/api/me",null,token).statusCode());
        token=(String)auth.login(email,p).get("token");
        assertEquals(200,request("POST","/api/auth/change-password",Map.of("currentPassword",p,"password",next,"confirmPassword",next),token).statusCode());
        assertEquals(401,request("GET","/api/me",null,token).statusCode());assertThrows(ApiError.class,()->auth.login(email,p));
        assertEquals(200,request("POST","/api/auth/forgot-password",Map.of("email",email),null).statusCode());
        String mail=Files.readString(Path.of("target/test-mailbox/"+id+".txt"));String reset=mail.split("#reset/")[1].trim();
        assertEquals(200,request("POST","/api/auth/reset-password",Map.of("token",reset,"password",p,"confirmPassword",p),null).statusCode());
        assertThrows(ApiError.class,()->auth.reset(reset,next,next));assertThrows(ApiError.class,()->auth.login(email,next));assertNotNull(auth.login(email,p).get("token"));
        db.update("UPDATE users SET role='ADMIN' WHERE id=?",id);String admin=(String)auth.login(email,p).get("token");
        assertEquals(200,request("GET","/api/me",null,admin).statusCode());
        db.update("UPDATE users SET active=FALSE WHERE id=?",id);assertEquals(401,request("GET","/api/me",null,admin).statusCode());
    }
    @Test void expiredResetAndSessionRejected()throws Exception{
        String id=Db.str(auth.register("Two","two@example.test","Local-password-123","Local-password-123"),"id");String t=(String)auth.login("two@example.test","Local-password-123").get("token");
        db.update("UPDATE sessions SET expires_at=? WHERE user_id=?",java.sql.Timestamp.valueOf("2000-01-01 00:00:00"),id);assertThrows(ApiError.class,()->auth.authenticate(t));
        auth.forgot("two@example.test");db.update("UPDATE password_reset_tokens SET expires_at=? WHERE user_id=?",java.sql.Timestamp.valueOf("2000-01-01 00:00:00"),id);
        String expired=Files.readString(Path.of("target/test-mailbox/"+id+".txt")).split("#reset/")[1].trim();assertThrows(ApiError.class,()->auth.reset(expired,"Local-password-123","Local-password-123"));
        assertThrows(ApiError.class,()->auth.reset("invalid","Local-password-123","Local-password-123"));
    }
}
