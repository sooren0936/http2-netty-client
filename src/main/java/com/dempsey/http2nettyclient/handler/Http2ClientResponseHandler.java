package com.dempsey.http2nettyclient.handler;

import com.dempsey.http2nettyclient.model.MapValues;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Http2ClientResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http2ClientResponseHandler.class);

    private final Map<Integer, MapValues> streamIdMap;

    public Http2ClientResponseHandler() {
        streamIdMap = new HashMap<>();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpResponse msg) {
        final Integer streamId = msg.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
        if (streamId == null) {
            LOGGER.error("HttpResponseHandler unexpected message received: {}", msg);
            return;
        }

        final MapValues mapValues = streamIdMap.get(streamId);

        if (mapValues == null) {
            LOGGER.error("Message received for unknown stream id {}", streamId);
        } else {
            final ByteBuf content = msg.content();
            if (content.isReadable()) {
                final int contentLength = content.readableBytes();
                final byte[] arr = new byte[contentLength];
                content.readBytes(arr);
                final String response = new String(arr, 0, contentLength, CharsetUtil.UTF_8);
                LOGGER.info(response);
                mapValues.setResponse(response);
            }

            mapValues.promise().setSuccess();
        }
    }

    public void put(final int streamId, final ChannelFuture writeFuture, final ChannelPromise promise) {
        streamIdMap.put(streamId, new MapValues(writeFuture, promise));
    }

    public String awaitResponses(final long timeout, final TimeUnit unit) {
        final Iterator<Map.Entry<Integer, MapValues>> itr = streamIdMap.entrySet().iterator();
        String response = null;

        while (itr.hasNext()) {
            final Map.Entry<Integer, MapValues> entry = itr.next();
            final ChannelFuture writeFuture = entry.getValue().writeFuture();
            final ChannelPromise promise = entry.getValue().promise();

            checkChannel(timeout, unit, entry, writeFuture, promise);
            LOGGER.info("---Stream id: {} received---", entry.getKey());
            response = entry.getValue().response();

            itr.remove();
        }
        return response;
    }

    private static void checkChannel(final long timeout, final TimeUnit unit,
                                     final Map.Entry<Integer, MapValues> entry,
                                     final ChannelFuture writeFuture, final ChannelPromise promise) {
        if (!writeFuture.awaitUninterruptibly(timeout, unit)) {
            throw new IllegalStateException("Timed out waiting to write for stream id " + entry.getKey());
        }
        if (!writeFuture.isSuccess()) {
            throw new RuntimeException(writeFuture.cause());
        }

        if (!promise.awaitUninterruptibly(timeout, unit)) {
            throw new IllegalStateException("Timed out waiting for response on stream id " + entry.getKey());
        }
        if (!promise.isSuccess()) {
            throw new RuntimeException(promise.cause());
        }
    }
}