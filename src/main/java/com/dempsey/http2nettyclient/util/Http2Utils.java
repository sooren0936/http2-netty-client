package com.dempsey.http2nettyclient.util;

import com.dempsey.http2nettyclient.adapter.Http2Adapter;
import com.dempsey.http2nettyclient.client.Http2ClientInitializer;
import com.dempsey.http2nettyclient.handler.Http2ClientResponseHandler;
import com.dempsey.http2nettyclient.handler.Http2SettingsHandler;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

import static io.netty.handler.logging.LogLevel.INFO;

/**
 * @author Suren Kalaychyan
 */
public final class Http2Utils {

    private Http2Utils() {
    }

    public static ApplicationProtocolNegotiationHandler getClientAPNHandler(final int maxContentLength,
                                                                            final Http2SettingsHandler settingsHandler,
                                                                            final Http2ClientResponseHandler responseHandler) {
        final Http2FrameLogger logger = new Http2FrameLogger(INFO, Http2ClientInitializer.class);
        final Http2Connection connection = new DefaultHttp2Connection(false);

        final InboundHttp2ToHttpAdapter inboundHttp2ToHttpAdapter = new InboundHttp2ToHttpAdapterBuilder(connection)
                .maxContentLength(maxContentLength)
                .propagateSettings(true)
                .build();
        final DelegatingDecompressorFrameListener delegatingDecompressorFrameListener = new DelegatingDecompressorFrameListener(connection, inboundHttp2ToHttpAdapter);
        final HttpToHttp2ConnectionHandler connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
                .frameListener(delegatingDecompressorFrameListener)
                .frameLogger(logger)
                .connection(connection)
                .build();

        return new Http2Adapter(ApplicationProtocolNames.HTTP_2, settingsHandler, responseHandler, connectionHandler);
    }

    public static FullHttpRequest createGetRequest(final String host, final int port) {
        final FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.valueOf("HTTP/2.0"), HttpMethod.GET, "/", Unpooled.EMPTY_BUFFER);
        final HttpHeaders headers = request.headers();
        headers.add(HttpHeaderNames.HOST, host + ":" + port)
                .add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HttpScheme.HTTPS)
                .add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP)
                .add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
        return request;
    }


    public static SslContext createSSLContext(final boolean isServer) throws SSLException, CertificateException {
        final SelfSignedCertificate ssc = new SelfSignedCertificate();

        SslContext sslCtx;

        if (isServer) {
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                    .sslProvider(SslProvider.JDK)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1))
                    .build();
        } else {
            sslCtx = SslContextBuilder.forClient()
                    .sslProvider(SslProvider.JDK)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2))
                    .build();
        }
        return sslCtx;
    }
}
