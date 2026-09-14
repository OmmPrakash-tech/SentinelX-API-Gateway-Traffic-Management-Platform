package com.example.backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class OperationsController {
    final Db db;final Registry registry;final AdminController audit;
    public OperationsController(Db db,Registry registry,AdminController audit){this.db=db;this.registry=registry;this.audit=audit;}
    record ServiceInput(@NotBlank @Size(max=100)String name,boolean enabled,@NotNull @Pattern(regexp="ROUND_ROBIN|LEAST_CONNECTIONS")String strategy){}
    record InstanceInput(@NotBlank String serviceId,@NotBlank String baseUrl,@NotBlank @Pattern(regexp="/[a-zA-Z0-9/_-]*")String healthPath,boolean enabled){}
    record RouteInput(@NotNull @Pattern(regexp="/[a-zA-Z0-9_-]+(?:/[a-zA-Z0-9_-]+)*")String pathPrefix,@NotBlank @Pattern(regexp="(?:GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)(?:,(?:GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS))*")String methods,@NotBlank String serviceId,boolean enabled,@Min(100) @Max(30000)int timeoutMs,@Min(0) @Max(3)int retries,@Min(0) @Max(1000)int backoffMs,@Min(1) @Max(100)int failureThreshold,@Min(100) @Max(300000)int recoveryMs,@Min(100) @Max(10000)int connectTimeoutMs,@Min(100) @Max(30000)int readTimeoutMs){}
    record RateInput(@NotNull @Pattern(regexp="GLOBAL|IP|USER|KEY|ROUTE")String scope,@Min(1) @Max(100000)int capacity,@DecimalMin("0.001") @DecimalMax("100000")double refillPerSecond,boolean enabled){}
    @GetMapping("/health") Object health(){db.jdbc.queryForObject("SELECT 1",Integer.class);return Map.of("status","UP","application","SentinelX");}
    @GetMapping("/api/ops/services") Object services(){return db.rows("SELECT * FROM services ORDER BY name");}
    @GetMapping("/api/ops/instances") Object instances(){return registry.instances();}
    @GetMapping("/api/ops/routes") Object routes(){return db.rows("SELECT r.*,s.name AS service_name FROM routes r JOIN services s ON s.id=r.service_id ORDER BY path_prefix");}
    @GetMapping("/api/me/apis") Object apis(){return db.rows("SELECT r.path_prefix,r.methods,s.name AS service_name FROM routes r JOIN services s ON s.id=r.service_id WHERE r.enabled=TRUE AND s.enabled=TRUE ORDER BY path_prefix");}
    @GetMapping("/api/ops/rate-limits") Object rates(){return db.rows("SELECT * FROM rate_limit_policies ORDER BY scope");}
    @GetMapping("/api/ops/circuits") Object circuits(){var result=new ArrayList<Map<String,Object>>();registry.circuits.forEach((key,c)->{var m=new LinkedHashMap<String,Object>(c.snapshot());m.put("id",key);result.add(m);});return result;}
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/api/admin/services") Object service(@Valid @RequestBody ServiceInput b,HttpServletRequest r){String id=Db.id();db.update("INSERT INTO services VALUES(?,?,?,?,?)",id,b.name,b.enabled,b.strategy,Db.now());audit.audit(r,"ADMIN_CREATED_SERVICE",id);return Map.of("id",id);}
    @org.springframework.transaction.annotation.Transactional
    @PutMapping("/api/admin/services/{id}") Object serviceUpdate(@PathVariable String id,@Valid @RequestBody ServiceInput b,HttpServletRequest r){db.one("SELECT id FROM services WHERE id=?",id);db.update("UPDATE services SET name=?,enabled=?,strategy=? WHERE id=?",b.name,b.enabled,b.strategy,id);audit.audit(r,"ADMIN_UPDATED_SERVICE",id);return Map.of("id",id);}
    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/api/admin/services/{id}") Object serviceDelete(@PathVariable String id,HttpServletRequest r){db.update("DELETE FROM services WHERE id=?",id);audit.audit(r,"ADMIN_DELETED_SERVICE",id);return Map.of("message","Service removed");}
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/api/admin/instances") Object instance(@Valid @RequestBody InstanceInput b,HttpServletRequest r){registry.validate(b.baseUrl);String id=Db.id();db.update("INSERT INTO service_instances VALUES(?,?,?,?,?,?)",id,b.serviceId,b.baseUrl.replaceAll("/$",""),b.healthPath,b.enabled,Db.now());audit.audit(r,"ADMIN_REGISTERED_INSTANCE",id);return Map.of("id",id);}
    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/api/admin/instances/{id}") Object instanceDelete(@PathVariable String id,HttpServletRequest r){db.update("DELETE FROM service_instances WHERE id=?",id);registry.health.remove(id);audit.audit(r,"ADMIN_DEREGISTERED_INSTANCE",id);return Map.of("message","Instance removed");}
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/api/ops/check-health") Object check(){registry.check();return Map.of("message","Health checks completed");}
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/api/admin/routes") Object route(@Valid @RequestBody RouteInput b,HttpServletRequest r){String id=Db.id();db.update("INSERT INTO routes(id,path_prefix,methods,service_id,enabled,timeout_ms,retries,backoff_ms,failure_threshold,recovery_ms,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",id,b.pathPrefix,b.methods,b.serviceId,b.enabled,b.timeoutMs,b.retries,b.backoffMs,b.failureThreshold,b.recoveryMs,Db.now());db.update("UPDATE routes SET connect_timeout_ms=?,read_timeout_ms=? WHERE id=?",b.connectTimeoutMs,b.readTimeoutMs,id);audit.audit(r,"ADMIN_CREATED_ROUTE",id);return Map.of("id",id);}
    @org.springframework.transaction.annotation.Transactional
    @PutMapping("/api/admin/routes/{id}") Object routeUpdate(@PathVariable String id,@Valid @RequestBody RouteInput b,HttpServletRequest r){db.one("SELECT id FROM routes WHERE id=?",id);db.update("UPDATE routes SET path_prefix=?,methods=?,service_id=?,enabled=?,timeout_ms=?,retries=?,backoff_ms=?,failure_threshold=?,recovery_ms=? WHERE id=?",b.pathPrefix,b.methods,b.serviceId,b.enabled,b.timeoutMs,b.retries,b.backoffMs,b.failureThreshold,b.recoveryMs,id);db.update("UPDATE routes SET connect_timeout_ms=?,read_timeout_ms=? WHERE id=?",b.connectTimeoutMs,b.readTimeoutMs,id);audit.audit(r,"ADMIN_UPDATED_ROUTE",id);return Map.of("id",id);}
    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/api/admin/routes/{id}") Object routeDelete(@PathVariable String id,HttpServletRequest r){db.update("DELETE FROM routes WHERE id=?",id);audit.audit(r,"ADMIN_DELETED_ROUTE",id);return Map.of("message","Route removed");}
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/api/admin/rate-limits") Object rate(@Valid @RequestBody RateInput b,HttpServletRequest r){String id=Db.id();db.update("INSERT INTO rate_limit_policies VALUES(?,?,?,?,?)",id,b.scope,b.capacity,b.refillPerSecond,b.enabled);audit.audit(r,"ADMIN_CREATED_RATE_LIMIT",id);return Map.of("id",id);}
    @org.springframework.transaction.annotation.Transactional
    @PutMapping("/api/admin/rate-limits/{id}") Object rateUpdate(@PathVariable String id,@Valid @RequestBody RateInput b,HttpServletRequest r){db.update("UPDATE rate_limit_policies SET scope=?,capacity=?,refill_per_second=?,enabled=? WHERE id=?",b.scope,b.capacity,b.refillPerSecond,b.enabled,id);audit.audit(r,"ADMIN_UPDATED_RATE_LIMIT",id);return Map.of("id",id);}
    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/api/admin/rate-limits/{id}") Object rateDelete(@PathVariable String id,HttpServletRequest r){db.update("DELETE FROM rate_limit_policies WHERE id=?",id);audit.audit(r,"ADMIN_DELETED_RATE_LIMIT",id);return Map.of("message","Policy removed");}
}

