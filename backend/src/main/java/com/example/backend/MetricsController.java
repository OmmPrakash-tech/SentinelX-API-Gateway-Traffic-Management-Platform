package com.example.backend;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@RestController
public class MetricsController {
    final Db db;final Registry registry;final long started=System.currentTimeMillis();
    public MetricsController(Db db,Registry registry){this.db=db;this.registry=registry;}
    @GetMapping("/api/ops/metrics") Object all(){return metrics(null);}
    @GetMapping("/api/me/metrics") Object own(HttpServletRequest r){return metrics(AccountController.user(r));}
    @GetMapping("/api/ops/logs") Object logs(){return db.rows("SELECT * FROM request_logs ORDER BY created_at DESC LIMIT 1000");}
    @GetMapping("/api/me/requests") Object requests(HttpServletRequest r){return db.rows("SELECT * FROM request_logs WHERE user_id=? ORDER BY created_at DESC LIMIT 1000",AccountController.user(r));}
    Map<String,Object> metrics(String user){
        String where=user==null?"":" WHERE user_id=?";Object[] args=user==null?new Object[]{}:new Object[]{user};
        var stats=db.one("SELECT COUNT(*) AS total,COALESCE(SUM(CASE WHEN status<400 THEN 1 ELSE 0 END),0) AS successes,COALESCE(SUM(CASE WHEN status>=400 THEN 1 ELSE 0 END),0) AS errors,COALESCE(SUM(CASE WHEN status=429 THEN 1 ELSE 0 END),0) AS violations,COALESCE(AVG(latency_ms),0) AS average_latency,COALESCE(SUM(retries),0) AS retries FROM request_logs"+where,args);
        var recent=db.rows("SELECT status,latency_ms,created_at FROM request_logs"+where+" ORDER BY created_at DESC LIMIT 10000",args);
        long now=System.currentTimeMillis(),today=LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        var series=new ArrayList<Map<String,Object>>();for(int i=11;i>=0;i--){long start=(now/300000-i)*300000;long count=recent.stream().filter(r->{long t=((Timestamp)r.get("created_at")).getTime();return t>=start&&t<start+300000;}).count();series.add(Map.of("time",start,"requests",count));}
        double[] latencies=recent.stream().mapToDouble(r->((Number)r.get("latency_ms")).doubleValue()).sorted().toArray();
        stats.put("p95",latencies.length==0?0:latencies[(int)Math.ceil(latencies.length*.95)-1]);stats.put("series",series);
        Object[] todayArgs=user==null?new Object[]{new Timestamp(today)}:new Object[]{user,new Timestamp(today)};
        stats.put("today",db.one("SELECT COUNT(*) AS n FROM request_logs"+where+(user==null?" WHERE":" AND")+" created_at>=?",todayArgs).get("n"));
        stats.put("requestsPerSecond",recent.stream().filter(r->((Timestamp)r.get("created_at")).getTime()>now-60000).count()/60d);
        stats.put("sampleNote","p95, chart and throughput use the latest 10,000 retained requests; counters cover retained logs (30 days).");
        if(user==null){stats.put("authenticationFailures",db.one("SELECT COUNT(*) AS n FROM system_events WHERE event_type='AUTHENTICATION_FAILURE'").get("n"));stats.put("topConsumers",db.rows("SELECT u.name,COUNT(*) AS requests,SUM(CASE WHEN l.status=429 THEN 1 ELSE 0 END) AS violations FROM request_logs l JOIN users u ON u.id=l.user_id GROUP BY u.id,u.name ORDER BY requests DESC LIMIT 5"));}
        if(user==null){var instances=registry.instances();stats.put("healthyInstances",instances.stream().filter(r->r.get("status").equals("HEALTHY")&&Db.bool(r,"enabled")).count());stats.put("instances",instances.size());stats.put("openCircuits",registry.circuits.values().stream().filter(c->c.snapshot().get("state").equals("OPEN")).count());stats.put("activeConsumers",db.one("SELECT COUNT(*) AS n FROM api_keys k JOIN users u ON k.user_id=u.id WHERE k.active=TRUE AND k.revoked=FALSE AND k.expires_at>? AND u.active=TRUE AND u.api_enabled=TRUE",Db.now()).get("n"));stats.put("uptimeSeconds",(now-started)/1000);}
        return stats;
    }
    @Scheduled(fixedDelay=3600000) void retain(){db.update("DELETE FROM request_logs WHERE created_at<?",Timestamp.from(Instant.now().minus(Duration.ofDays(30))));db.update("DELETE FROM sessions WHERE expires_at<?",Db.now());db.update("DELETE FROM password_reset_tokens WHERE expires_at<?",Timestamp.from(Instant.now().minus(Duration.ofDays(1))));}
}
