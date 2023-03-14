package com.dempsey.http2nettyclient.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2Settings;

import java.util.concurrent.TimeUnit;

public class Http2SettingsHandler extends SimpleChannelInboundHandler<Http2Settings> {

    private final ChannelPromise promise;

    public Http2SettingsHandler(final ChannelPromise promise) {
        this.promise = promise;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Http2Settings msg) {
        promise.setSuccess();
        ctx.pipeline().remove(this);
    }

    public void awaitSettings(final long timeout, final TimeUnit unit) {
        if (!promise.awaitUninterruptibly(timeout, unit)) {
            throw new IllegalStateException("Timed out waiting for settings");
        }
    }
}