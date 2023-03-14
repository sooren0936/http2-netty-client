package com.dempsey.http2nettyclient.client;

import com.dempsey.http2nettyclient.handler.Http2ClientResponseHandler;
import com.dempsey.http2nettyclient.handler.Http2SettingsHandler;
import com.dempsey.http2nettyclient.util.Http2Utils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;

public class Http2ClientInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final int maxContentLength;
    private final String host;
    private final int port;

    private Http2SettingsHandler settingsHandler;
    private Http2ClientResponseHandler responseHandler;

    public Http2ClientInitializer(final SslContext sslCtx, final int maxContentLength,
                                  final String host, final int port) {
        this.sslCtx = sslCtx;
        this.maxContentLength = maxContentLength;
        this.host = host;
        this.port = port;
    }

    public Http2SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    public Http2ClientResponseHandler getResponseHandler() {
        return responseHandler;
    }

    @Override
    public void initChannel(final SocketChannel ch) {
        settingsHandler = new Http2SettingsHandler(ch.newPromise());
        responseHandler = new Http2ClientResponseHandler();

        if (sslCtx != null) {
            final ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(sslCtx.newHandler(ch.alloc(), host, port));
            pipeline.addLast(Http2Utils.getClientAPNHandler(maxContentLength, settingsHandler, responseHandler));
        }
    }
}