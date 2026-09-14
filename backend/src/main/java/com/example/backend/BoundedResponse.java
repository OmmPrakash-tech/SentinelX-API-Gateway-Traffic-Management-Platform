package com.example.backend;

import java.io.*;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;

/** Cancels upstream delivery before buffering more than the configured body limit. */
final class BoundedResponse implements HttpResponse.BodySubscriber<byte[]> {
    private final int limit;private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    private final CompletableFuture<byte[]> result=new CompletableFuture<>();private Flow.Subscription subscription;
    BoundedResponse(int limit,long timeoutMs){this.limit=limit;result.orTimeout(timeoutMs,TimeUnit.MILLISECONDS).whenComplete((v,e)->{if(e!=null&&subscription!=null)subscription.cancel();});}
    public CompletionStage<byte[]> getBody(){return result;}
    public void onSubscribe(Flow.Subscription s){subscription=s;s.request(1);}
    public void onNext(List<ByteBuffer> chunks){for(var chunk:chunks){if(chunk.remaining()>limit-bytes.size()){subscription.cancel();result.completeExceptionally(new IOException("Upstream response too large"));return;}byte[] data=new byte[chunk.remaining()];chunk.get(data);bytes.writeBytes(data);}subscription.request(1);}
    public void onError(Throwable error){result.completeExceptionally(error);}
    public void onComplete(){result.complete(bytes.toByteArray());}
}
