package com.example.backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AccountController {
    final AuthService auth; final Db db; final TrafficPrimitives.Limiter limiter=new TrafficPrimitives.Limiter();
    public AccountController(AuthService auth,Db db){this.auth=auth;this.db=db;}
    record Register(@NotBlank @Size(max=100)String name,@Email @NotBlank @Size(max=254)String email,@NotNull String password,@NotNull String confirmPassword){}
    record Login(@Email @NotBlank String email,@NotNull @Size(max=100)String password){}
    record EmailInput(@Email @NotBlank String email){}
    record Change(@NotNull String currentPassword,@NotNull String password,@NotNull String confirmPassword){}
    record Reset(@NotBlank String token,@NotNull String password,@NotNull String confirmPassword){}
    record Profile(@NotBlank @Size(max=100)String name){}
    static String user(HttpServletRequest r){return r.getUserPrincipal().getName();}
    void throttle(HttpServletRequest r){if(!limiter.take(r.getRemoteAddr(),10,0.2).allowed())throw new ApiError(429,"AUTH_RATE_LIMIT","Too many attempts; try again later");}
    @PostMapping("/auth/register") Object register(@Valid @RequestBody Register b,HttpServletRequest r){throttle(r);return auth.register(b.name,b.email,b.password,b.confirmPassword);}
    @PostMapping("/auth/login") Object login(@Valid @RequestBody Login b,HttpServletRequest r){throttle(r);try{return auth.login(b.email,b.password);}catch(ApiError e){db.update("INSERT INTO system_events VALUES(?,?,?,?)",Db.id(),"AUTHENTICATION_FAILURE",String.valueOf(r.getAttribute("requestId")),Db.now());throw e;}}
    @PostMapping("/auth/logout") Object logout(HttpServletRequest r){db.update("DELETE FROM sessions WHERE token_hash=?",AuthService.hash(r.getHeader("Authorization").substring(7)));return Map.of("message","Signed out");}
    @PostMapping("/auth/logout-all") Object logoutAll(HttpServletRequest r){auth.invalidate(user(r));return Map.of("message","All sessions revoked");}
    @PostMapping("/auth/change-password") Object change(@Valid @RequestBody Change b,HttpServletRequest r){auth.change(user(r),b.currentPassword,b.password,b.confirmPassword);return Map.of("message","Password changed. Sign in again.");}
    @PostMapping("/auth/forgot-password") Object forgot(@Valid @RequestBody EmailInput b,HttpServletRequest r){throttle(r);auth.forgot(b.email);return Map.of("message","If an active account exists, a reset link will be delivered.");}
    @PostMapping("/auth/reset-password") Object reset(@Valid @RequestBody Reset b,HttpServletRequest r){throttle(r);auth.reset(b.token,b.password,b.confirmPassword);return Map.of("message","Password reset. Sign in again.");}
    @GetMapping("/me") Object me(HttpServletRequest r){return auth.profile(user(r));}
    @PatchMapping("/me") Object profile(@Valid @RequestBody Profile b,HttpServletRequest r){db.update("UPDATE users SET name=?,updated_at=? WHERE id=?",b.name,Db.now(),user(r));return me(r);}
    @GetMapping("/me/sessions") Object sessions(HttpServletRequest r){return db.rows("SELECT created_at,expires_at FROM sessions WHERE user_id=? AND expires_at>? ORDER BY created_at DESC",user(r),Db.now());}
}

