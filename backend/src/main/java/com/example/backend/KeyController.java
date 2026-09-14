package com.example.backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/me/keys")
public class KeyController {
    final Db db;public KeyController(Db db){this.db=db;}
    static final String FIELDS="id,user_id,name,prefix,active,revoked,tier,created_at,expires_at";
    record Create(@NotBlank @Size(max=100)String name,@Min(1) @Max(365)int days){}
    record Status(boolean active){}
    @GetMapping Object list(HttpServletRequest r){return db.rows("SELECT "+FIELDS+",(SELECT COUNT(*) FROM request_logs l WHERE l.key_id=k.id) AS requests FROM api_keys k WHERE user_id=? ORDER BY created_at DESC",AccountController.user(r));}
    @org.springframework.transaction.annotation.Transactional
    @PostMapping Object create(@Valid @RequestBody Create b,HttpServletRequest r){return createKey(AccountController.user(r),b.name,b.days);}
    Map<String,Object> createKey(String user,String name,int days){
        String id=Db.id(),secret="sx_"+AuthService.secret();db.update("INSERT INTO api_keys(id,user_id,name,token_hash,prefix,created_at,expires_at) VALUES(?,?,?,?,?,?,?)",id,user,name,AuthService.hash(secret),secret.substring(0,12),Db.now(),Timestamp.from(Instant.now().plusSeconds(days*86400L)));
        return Map.of("id",id,"secret",secret,"message","Copy this key now. It will not be shown again.");
    }
    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/{id}") Object revoke(@PathVariable String id,HttpServletRequest r){owned(id,r);db.update("UPDATE api_keys SET active=FALSE,revoked=TRUE WHERE id=?",id);return Map.of("message","Key revoked");}
    @org.springframework.transaction.annotation.Transactional
    @PatchMapping("/{id}") Object status(@PathVariable String id,@RequestBody Status b,HttpServletRequest r){owned(id,r);db.update("UPDATE api_keys SET active=? WHERE id=? AND revoked=FALSE",b.active,id);return Map.of("message","Key updated");}
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/{id}/rotate") Object rotate(@PathVariable String id,HttpServletRequest r){var key=owned(id,r);db.update("UPDATE api_keys SET active=FALSE,revoked=TRUE WHERE id=?",id);return createKey(AccountController.user(r),Db.str(key,"name"),90);}
    Map<String,Object> owned(String id,HttpServletRequest r){return db.one("SELECT "+FIELDS+" FROM api_keys WHERE id=? AND user_id=?",id,AccountController.user(r));}
    Map<String,Object> validate(String secret){var found=db.rows("SELECT k.id AS key_id,u.id,u.role FROM api_keys k JOIN users u ON k.user_id=u.id WHERE k.token_hash=? AND k.active=TRUE AND k.revoked=FALSE AND k.expires_at>? AND u.active=TRUE AND u.api_enabled=TRUE AND u.reset_required=FALSE",AuthService.hash(secret),Db.now());if(found.isEmpty())throw new ApiError(401,"INVALID_API_KEY","API key is invalid, expired or revoked");return found.getFirst();}
}
