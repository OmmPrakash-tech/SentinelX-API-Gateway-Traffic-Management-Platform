package com.example.backend;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class AuthService {
    final Db db; final BCryptPasswordEncoder passwords; final String mailbox; final String dummy;
    public AuthService(Db db,BCryptPasswordEncoder passwords,@Value("${sentinel.reset-directory:}")String mailbox) {this.db=db;this.passwords=passwords;this.mailbox=mailbox;dummy=passwords.encode(secret());}
    static String secret(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    static void password(String p,String confirm){if(p==null||p.length()<12||p.getBytes(StandardCharsets.UTF_8).length>72||!p.equals(confirm))throw new ApiError(400,"PASSWORD_POLICY","Use 12–72 bytes and matching passwords");}
    @Transactional
    public Map<String,Object> register(String name,String email,String p,String confirm){
        password(p,confirm);String id=Db.id();
        db.update("INSERT INTO users(id,name,email,password_hash,role,created_at,updated_at) VALUES(?,?,?,?,?,?,?)",id,name,email.toLowerCase(Locale.ROOT).trim(),passwords.encode(p),"DEVELOPER",Db.now(),Db.now());
        return profile(id);
    }
    @Transactional
    public Map<String,Object> login(String email,String p){
        var found=db.rows("SELECT * FROM users WHERE email=?",email.toLowerCase(Locale.ROOT).trim());
        String encoded=found.isEmpty()?dummy:Db.str(found.getFirst(),"password_hash");
        if(!passwords.matches(p,encoded)||found.isEmpty()||!Db.bool(found.getFirst(),"active"))throw new ApiError(401,"INVALID_CREDENTIALS","Email or password is incorrect");
        var u=found.getFirst();if(Db.bool(u,"reset_required"))throw new ApiError(403,"RESET_REQUIRED","Reset your password before signing in");
        String token=secret(),id=Db.str(u,"id");var expires=Timestamp.from(Instant.now().plus(Duration.ofHours(8)));
        db.update("INSERT INTO sessions VALUES(?,?,?,?)",hash(token),id,Db.now(),expires);
        db.update("UPDATE users SET last_login=? WHERE id=?",Db.now(),id);
        return Map.of("token",token,"expiresAt",expires.toInstant().toString(),"user",profile(id));
    }
    Map<String,Object> authenticate(String token){
        var list=db.rows("SELECT u."+Db.USER_FIELDS.replace(",",",u.")+" FROM users u JOIN sessions s ON s.user_id=u.id WHERE s.token_hash=? AND s.expires_at>? AND u.active=TRUE AND u.reset_required=FALSE",hash(token),Db.now());
        if(list.isEmpty())throw new ApiError(401,"INVALID_SESSION","Session expired or revoked");return list.getFirst();
    }
    Map<String,Object> profile(String id){return db.one("SELECT "+Db.USER_FIELDS+" FROM users WHERE id=?",id);}
    @Transactional public void change(String id,String current,String p,String confirm){
        password(p,confirm);var u=db.one("SELECT * FROM users WHERE id=? FOR UPDATE",id);
        if(!passwords.matches(current,Db.str(u,"password_hash")))throw new ApiError(400,"WRONG_PASSWORD","Current password is incorrect");setPassword(id,p);
    }
    void setPassword(String id,String p){db.update("UPDATE users SET password_hash=?,reset_required=FALSE,updated_at=? WHERE id=?",passwords.encode(p),Db.now(),id);invalidate(id);db.update("UPDATE password_reset_tokens SET used_at=? WHERE user_id=? AND used_at IS NULL",Db.now(),id);}
    void invalidate(String id){db.update("DELETE FROM sessions WHERE user_id=?",id);}
    @Transactional public void forgot(String email){
        var list=db.rows("SELECT id FROM users WHERE email=? AND active=TRUE FOR UPDATE",email.toLowerCase(Locale.ROOT).trim());if(list.isEmpty())return;
        String id=Db.str(list.getFirst(),"id"),token=secret();
        db.update("UPDATE password_reset_tokens SET used_at=? WHERE user_id=? AND used_at IS NULL",Db.now(),id);
        db.update("INSERT INTO password_reset_tokens(token_hash,user_id,created_at,expires_at) VALUES(?,?,?,?)",hash(token),id,Db.now(),Timestamp.from(Instant.now().plusSeconds(900)));
        if(!mailbox.isBlank())try{Path dir=Path.of(mailbox);Files.createDirectories(dir);Files.writeString(dir.resolve(id+".txt"),"Local development password reset\nExpires in 15 minutes.\nhttp://localhost:8080/#reset/"+token+"\n",StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);}catch(Exception e){throw new IllegalStateException("Local reset delivery failed",e);}
    }
    @Transactional public void reset(String token,String p,String confirm){
        password(p,confirm);
        var list=db.rows("SELECT * FROM password_reset_tokens WHERE token_hash=? AND used_at IS NULL AND expires_at>? FOR UPDATE",hash(token),Db.now());
        if(list.isEmpty())throw new ApiError(400,"INVALID_RESET_TOKEN","Reset link is invalid or expired");
        setPassword(Db.str(list.getFirst(),"user_id"),p);
    }
}
