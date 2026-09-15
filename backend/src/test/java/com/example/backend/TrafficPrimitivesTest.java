package com.example.backend;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class TrafficPrimitivesTest {
    @Test void prefixBoundaryAndRetrySafety(){assertTrue(TrafficPrimitives.matches("/users","/users/1"));assertFalse(TrafficPrimitives.matches("/users","/users-other"));assertTrue(TrafficPrimitives.retryable("GET",503));assertFalse(TrafficPrimitives.retryable("POST",503));assertFalse(TrafficPrimitives.retryable("GET",400));}
    @Test void bucketRefillsWithMonotonicTime(){var time=new AtomicLong();var l=new TrafficPrimitives.Limiter(time::get);assertTrue(l.take("a",2,1).allowed());assertTrue(l.take("a",2,1).allowed());assertFalse(l.take("a",2,1).allowed());time.set(1_000_000_000L);assertTrue(l.take("a",2,1).allowed());assertFalse(l.take("a",2,1).allowed());}
    @Test void concurrentBucketCannotOverspend()throws Exception{var l=new TrafficPrimitives.Limiter(()->0L);var successes=new AtomicInteger();try(var pool=Executors.newVirtualThreadPerTaskExecutor()){for(int i=0;i<1000;i++)pool.submit(()->{if(l.take("shared",100,1).allowed())successes.incrementAndGet();});}assertEquals(100,successes.get());}
    @Test void roundRobinAndLeastConnections(){var b=new TrafficPrimitives.Balancer();var ids=List.of("A","B","C");for(int i=0;i<9;i++)assertEquals(ids.get(i%3),b.acquire(ids,"ROUND_ROBIN"));b.release("C");assertEquals("C",b.acquire(ids,"LEAST_CONNECTIONS"));assertEquals(3,b.active("C"));}
    @Test void concurrentReservationsAndReleases()throws Exception{var b=new TrafficPrimitives.Balancer();var ids=List.of("A","B");try(var pool=Executors.newVirtualThreadPerTaskExecutor()){for(int i=0;i<5000;i++)pool.submit(()->{String id=b.acquire(ids,"LEAST_CONNECTIONS");b.release(id);});}assertEquals(0,b.active("A"));assertEquals(0,b.active("B"));}
    @Test void circuitAllowsOnlyOneRecoveryProbe(){var c=new TrafficPrimitives.Circuit();assertTrue(c.allow(0,100));c.finish(false,1,0);assertFalse(c.allow(99,100));assertTrue(c.allow(100,100));assertFalse(c.allow(100,100));assertEquals("HALF_OPEN",c.snapshot().get("state"));c.finish(true,1,100);assertEquals("CLOSED",c.snapshot().get("state"));assertTrue(c.allow(101,100));}
    @Test void staleSuccessCannotCloseNewCircuit(){var c=new TrafficPrimitives.Circuit();long first=c.acquire(0,100),second=c.acquire(0,100);c.complete(first,false,1,0);c.complete(second,true,1,1);assertEquals("OPEN",c.snapshot().get("state"));}
}

