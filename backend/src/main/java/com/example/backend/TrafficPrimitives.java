package com.example.backend;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

public final class TrafficPrimitives {
    private TrafficPrimitives(){}
    public record Quota(boolean allowed,long remaining,long retryAfter){}
    public static final class Limiter {
        private final ConcurrentHashMap<String,Bucket> buckets=new ConcurrentHashMap<>();
        private final LongSupplier clock;
        public Limiter(){this(System::nanoTime);} public Limiter(LongSupplier clock){this.clock=clock;}
        public Quota peek(String key,int capacity,double refill){Bucket b=buckets.get(key);if(b==null)return new Quota(true,capacity,0);synchronized(b){double available=Math.min(capacity,b.tokens+Math.max(0,clock.getAsLong()-b.last)/1e9*refill);return new Quota(available>=1,(long)available,available>=1?0:Math.max(1,(long)Math.ceil((1-available)/refill)));}}
        public Quota take(String key,int capacity,double refill){
            if(buckets.size()>100000){long now=clock.getAsLong();buckets.entrySet().removeIf(e->now-e.getValue().last>3_600_000_000_000L);if(buckets.size()>100000&&!buckets.containsKey(key))return new Quota(false,0,60);}
            return buckets.computeIfAbsent(key,k->new Bucket(capacity,clock.getAsLong())).take(capacity,refill,clock.getAsLong());
        }
        static final class Bucket {
            double tokens;volatile long last;Bucket(int n,long now){tokens=n;last=now;}
            synchronized Quota take(int capacity,double refill,long now){tokens=Math.min(capacity,tokens+Math.max(0,now-last)/1e9*refill);last=now;if(tokens>=1){tokens--;return new Quota(true,(long)tokens,0);}return new Quota(false,0,Math.max(1,(long)Math.ceil((1-tokens)/refill)));}
        }
    }
    public static final class Balancer {
        private long cursor;private final Map<String,Integer> active=new HashMap<>();
        public synchronized String acquire(List<String> ids,String strategy){if(ids.isEmpty())throw new ApiError(503,"NO_HEALTHY_INSTANCE","No healthy upstream instance");String id;
            if("LEAST_CONNECTIONS".equals(strategy)){int min=ids.stream().mapToInt(x->active.getOrDefault(x,0)).min().orElse(0);var eligible=ids.stream().filter(x->active.getOrDefault(x,0)==min).toList();id=eligible.get((int)Math.floorMod(cursor++,eligible.size()));}
            else id=ids.get((int)Math.floorMod(cursor++,ids.size()));active.merge(id,1,Integer::sum);return id;
        }
        public synchronized void release(String id){active.computeIfPresent(id,(k,v)->v<=1?null:v-1);}
        public synchronized int active(String id){return active.getOrDefault(id,0);}
    }
    public static final class Circuit {
        private String state="CLOSED";private int failures;private long opened;private boolean probe;private long transitions;private long generation;
        public synchronized boolean available(long now,long recovery){return state.equals("CLOSED")||(state.equals("OPEN")&&now-opened>=recovery)||(state.equals("HALF_OPEN")&&!probe);}
        public synchronized long acquire(long now,long recovery){return allow(now,recovery)?generation:-1;}
        public synchronized void complete(long permit,boolean ok,int threshold,long now){if(permit==generation)finish(ok,threshold,now);}
        public synchronized boolean allow(long now,long recovery){if(state.equals("OPEN")&&now-opened>=recovery){state="HALF_OPEN";transitions++;}if(state.equals("OPEN"))return false;if(state.equals("HALF_OPEN")){if(probe)return false;probe=true;}return true;}
        public synchronized void finish(boolean ok,int threshold,long now){probe=false;if(ok){failures=0;if(!state.equals("CLOSED")){state="CLOSED";transitions++;generation++;}}else{failures++;if(state.equals("HALF_OPEN")||failures>=threshold){if(!state.equals("OPEN"))transitions++;state="OPEN";opened=now;generation++;}}}
        public synchronized Map<String,Object> snapshot(){return Map.of("state",state,"failures",failures,"openedAt",opened,"transitions",transitions);}
    }
    public static boolean matches(String prefix,String path){return path.equals(prefix)||path.startsWith(prefix+"/");}
    public static boolean retryable(String method,int status){return Set.of("GET","HEAD","OPTIONS").contains(method)&&(status==502||status==503||status==504);}
}
