package com.dempsey.http2nettyclient.adapter;

import com.dempsey.http2nettyclient.handler.Http2ClientResponseHandler;
import com.dempsey.http2nettyclient.handler.Http2SettingsHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

/**
 * @author Suren Kalaychyan
 */
public class Http2Adapter extends ApplicationProtocolNegotiationHandler {

    private final Http2SettingsHandler settingsHandler;
    private final Http2ClientResponseHandler responseHandler;
    private final HttpToHttp2ConnectionHandler connectionHandler;

    public Http2Adapter(final String fallbackProtocol, final Http2SettingsHandler settingsHandler,
                        final Http2ClientResponseHandler responseHandler, final HttpToHttp2ConnectionHandler connectionHandler) {
        super(fallbackProtocol);
        this.settingsHandler = settingsHandler;
        this.responseHandler = responseHandler;
        this.connectionHandler = connectionHandler;
    }

    @Override
    protected void configurePipeline(final ChannelHandlerContext ctx, final String protocol) {
        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            ChannelPipeline p = ctx.pipeline();
            p.addLast(connectionHandler);
            p.addLast(settingsHandler, responseHandler);
        } else {
            ctx.close();
            throw new IllegalStateException("Protocol: " + protocol + " not supported");
        }
    }
}
