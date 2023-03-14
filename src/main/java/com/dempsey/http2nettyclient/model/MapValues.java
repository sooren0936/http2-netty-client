package com.dempsey.http2nettyclient.model;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;

public class MapValues {

    private final ChannelFuture writeFuture;
    private final ChannelPromise promise;

    private String response;

    public MapValues(final ChannelFuture writeFuture, final ChannelPromise promise) {
        this.writeFuture = writeFuture;
        this.promise = promise;
    }

    public ChannelFuture writeFuture() {
        return writeFuture;
    }

    public ChannelPromise promise() {
        return promise;
    }

    public String response() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}