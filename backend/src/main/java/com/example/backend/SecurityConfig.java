package com.example.backend;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;

@Configuration
public class SecurityConfig {
    @Bean org.springframework.security.core.userdetails.UserDetailsService noDefaultUsers(){return name->{throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Bearer authentication required");};}
    @Bean BCryptPasswordEncoder encoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain security(HttpSecurity http,AuthService auth,ObjectMapper json) throws Exception {
        var filter=new OncePerRequestFilter() {
            protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws IOException,ServletException {
                String incoming=req.getHeader("X-Request-ID");
                String id=incoming!=null&&incoming.matches("[a-zA-Z0-9._-]{1,64}")?incoming:Db.id();
                req.setAttribute("requestId",id);res.setHeader("X-Request-ID",id);
                try {
                    String header=req.getHeader("Authorization");
                    if(header!=null&&header.startsWith("Bearer ")) {
                        var user=auth.authenticate(header.substring(7)); req.setAttribute("user",user);
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(Db.str(user,"id"),null,List.of(new SimpleGrantedAuthority("ROLE_"+Db.str(user,"role")))));
                    }
                    chain.doFilter(req,res);
                } catch(ApiError e) { res.setStatus(e.status);res.setContentType("application/json");json.writeValue(res.getOutputStream(),Errors.body(e.status,e.code,e.getMessage(),id)); }
                finally { SecurityContextHolder.clearContext(); }
            }
        };
        return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(f->f.disable()).httpBasic(b->b.disable())
            .authorizeHttpRequests(a->a.requestMatchers("/","/index.html","/app.js","/style.css","/favicon.svg","/api/auth/register","/api/auth/login","/api/auth/forgot-password","/api/auth/reset-password","/health","/gateway/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/ops/**").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/me/keys","/api/me/keys/*/rotate").hasAnyRole("ADMIN","DEVELOPER")
                .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->{s.setStatus(401);s.setContentType("application/json");json.writeValue(s.getOutputStream(),Errors.body(401,"UNAUTHENTICATED","Sign in to continue",String.valueOf(r.getAttribute("requestId"))));})
                .accessDeniedHandler((r,s,x)->{s.setStatus(403);s.setContentType("application/json");json.writeValue(s.getOutputStream(),Errors.body(403,"FORBIDDEN","This action requires a different role",String.valueOf(r.getAttribute("requestId"))));}))
            .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
    }
}
