package com.example.backend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.*;
import java.sql.Timestamp;
import java.time.Instant;

@Component
public class Db {
    final JdbcTemplate jdbc;
    public Db(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    List<Map<String,Object>> rows(String sql, Object... args) { return jdbc.queryForList(sql, args); }
    Map<String,Object> one(String sql, Object... args) {
        var rows = rows(sql,args); if(rows.isEmpty()) throw new ApiError(404,"NOT_FOUND","Resource not found"); return rows.getFirst();
    }
    int update(String sql,Object...args) { return jdbc.update(sql,args); }
    static String id() { return UUID.randomUUID().toString(); }
    static Timestamp now() { return Timestamp.from(Instant.now()); }
    static String str(Map<String,Object> m,String k) { return String.valueOf(m.get(k)); }
    static int num(Map<String,Object> m,String k) { return ((Number)m.get(k)).intValue(); }
    static boolean bool(Map<String,Object> m,String k) { return Boolean.TRUE.equals(m.get(k)); }
    static final String USER_FIELDS="id,name,email,role,active,api_enabled,reset_required,created_at,updated_at,last_login";
}
