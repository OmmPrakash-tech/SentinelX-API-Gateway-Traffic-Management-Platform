package com.example.backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    final Db db;final AuthService auth;public AdminController(Db db,AuthService auth){this.db=db;this.auth=auth;}
    record UserChange(@Pattern(regexp="ADMIN|OPERATOR|DEVELOPER|USER") @NotNull String role,boolean active,boolean apiEnabled){}
    record Tier(@Pattern(regexp="STANDARD|ELEVATED") @NotNull String tier,boolean active){}
    @GetMapping("/users") Object users(){return db.rows("SELECT "+Db.USER_FIELDS+",(SELECT COUNT(*) FROM request_logs l WHERE l.user_id=u.id) AS requests,(SELECT COUNT(*) FROM api_keys k WHERE k.user_id=u.id AND k.active=TRUE AND k.revoked=FALSE) AS active_keys FROM users u ORDER BY created_at DESC LIMIT 1000");}
    @org.springframework.transaction.annotation.Transactional
    @PatchMapping("/users/{id}") Object change(@PathVariable String id,@Valid @RequestBody UserChange b,HttpServletRequest r){
        auth.profile(id);if(id.equals(AccountController.user(r))&&(!b.active||!b.role.equals("ADMIN")))throw new ApiError(400,"SELF_LOCKOUT","You cannot deactivate or demote your own administrator account");
        db.update("UPDATE users SET role=?,active=?,api_enabled=?,updated_at=? WHERE id=?",b.role,b.active,b.apiEnabled,Db.now(),id);auth.invalidate(id);audit(r,"ADMIN_UPDATED_USER",id);return auth.profile(id);
    }
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/users/{id}/force-logout") Object logout(@PathVariable String id,HttpServletRequest r){auth.profile(id);auth.invalidate(id);audit(r,"ADMIN_FORCED_LOGOUT",id);return Map.of("message","Sessions invalidated");}
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/users/{id}/force-reset") Object reset(@PathVariable String id,HttpServletRequest r){auth.profile(id);db.update("UPDATE users SET reset_required=TRUE,updated_at=? WHERE id=?",Db.now(),id);auth.invalidate(id);db.update("UPDATE password_reset_tokens SET used_at=? WHERE user_id=? AND used_at IS NULL",Db.now(),id);audit(r,"ADMIN_FORCED_PASSWORD_RESET",id);return Map.of("message","User must request a reset link and establish a new password");}
    @GetMapping("/consumers") Object consumers(){return db.rows("SELECT "+KeyController.FIELDS+",(SELECT email FROM users u WHERE u.id=k.user_id) AS email,(SELECT COUNT(*) FROM request_logs l WHERE l.key_id=k.id) AS requests FROM api_keys k ORDER BY created_at DESC LIMIT 1000");}
    @org.springframework.transaction.annotation.Transactional
    @PatchMapping("/consumers/{id}") Object consumer(@PathVariable String id,@Valid @RequestBody Tier b,HttpServletRequest r){db.one("SELECT id FROM api_keys WHERE id=?",id);db.update("UPDATE api_keys SET tier=?,active=? WHERE id=? AND revoked=FALSE",b.tier,b.active,id);audit(r,"ADMIN_UPDATED_API_CONSUMER",id);return Map.of("message","Consumer updated");}
    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/consumers/{id}") Object revoke(@PathVariable String id,HttpServletRequest r){db.one("SELECT id FROM api_keys WHERE id=?",id);db.update("UPDATE api_keys SET active=FALSE,revoked=TRUE WHERE id=?",id);audit(r,"ADMIN_REVOKED_API_KEY",id);return Map.of("message","Consumer revoked");}
    @GetMapping("/audit") Object auditLog(){return db.rows("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 1000");}
    void audit(HttpServletRequest r,String action,String resource){db.update("INSERT INTO audit_logs VALUES(?,?,?,?,?,?)",Db.id(),AccountController.user(r),action,resource,String.valueOf(r.getAttribute("requestId")),Db.now());}
}
