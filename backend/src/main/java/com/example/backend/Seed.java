package com.example.backend;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;

@Component
public class Seed implements ApplicationRunner {
    final Db db;final AuthService auth;final boolean enabled;final String host;
    public Seed(Db db,AuthService auth,@Value("${sentinel.seed}")boolean enabled,@Value("${sentinel.demo-host}")String host){this.db=db;this.auth=auth;this.enabled=enabled;this.host=host;}
    public void run(ApplicationArguments args){if(!enabled)return;
        for(String role:List.of("ADMIN","OPERATOR","DEVELOPER","USER")){String email=role.toLowerCase(Locale.ROOT)+"@sentinelx.local";if(db.rows("SELECT id FROM users WHERE email=?",email).isEmpty()){var u=auth.register(role.charAt(0)+role.substring(1).toLowerCase(Locale.ROOT)+" Demo",email,"SentinelX-Local-2026!","SentinelX-Local-2026!");db.update("UPDATE users SET role=? WHERE id=?",role,u.get("id"));}}
        int port=9101;for(String name:List.of("user","product","order")){if(db.rows("SELECT id FROM services WHERE name=?",name+"-service").isEmpty()){
            String id=Db.id();db.update("INSERT INTO services VALUES(?,?,?,?,?)",id,name+"-service",true,"ROUND_ROBIN",Db.now());
            String origin=host.equals("docker")?"http://"+name+"-service:9100":"http://"+host+":"+port;
            db.update("INSERT INTO service_instances VALUES(?,?,?,?,?,?)",Db.id(),id,origin,"/health",true,Db.now());
            if(name.equals("user"))db.update("INSERT INTO service_instances VALUES(?,?,?,?,?,?)",Db.id(),id,host.equals("docker")?"http://user-service-2:9100":"http://"+host+":9104","/health",true,Db.now());
            db.update("INSERT INTO routes(id,path_prefix,methods,service_id,enabled,timeout_ms,retries,backoff_ms,failure_threshold,recovery_ms,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",Db.id(),"/"+name+"s","GET,POST",id,true,3000,1,50,5,10000,Db.now());
        }port++;}
        for(String scope:List.of("GLOBAL","IP","USER","KEY","ROUTE"))if(db.rows("SELECT id FROM rate_limit_policies WHERE scope=?",scope).isEmpty())db.update("INSERT INTO rate_limit_policies VALUES(?,?,?,?,?)",Db.id(),scope,scope.equals("GLOBAL")?10000:1000,scope.equals("GLOBAL")?1000d:100d,true);
    }
}
