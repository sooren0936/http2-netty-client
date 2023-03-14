import com.dempsey.http2nettyclient.client.Http2ClientInitializer;
import com.dempsey.http2nettyclient.handler.Http2ClientResponseHandler;
import com.dempsey.http2nettyclient.handler.Http2SettingsHandler;
import com.dempsey.http2nettyclient.util.Http2Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.dempsey.http2nettyclient.util.Http2Utils.createSSLContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Suren Kalaychyan
 */
class ClientTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTest.class);

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8443;

    private EventLoopGroup workerGroup;
    private SslContext sslCtx;

    @BeforeEach
    public void setup() throws Exception {
        workerGroup = new NioEventLoopGroup();
        sslCtx = createSSLContext(false);
    }

    @AfterEach
    public void shutDown() {
        workerGroup.shutdownGracefully();
    }

    @Test
    void whenRequestSent_thenMessageReceived() {
        final Http2ClientInitializer initializer = new Http2ClientInitializer(sslCtx, Integer.MAX_VALUE, HOST, PORT);

        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .remoteAddress(HOST, PORT)
                .handler(initializer);

        final Channel channel = bootstrap.connect().syncUninterruptibly().channel();
        LOGGER.info("Connected to [" + HOST + ':' + PORT + ']');

        final Http2SettingsHandler http2SettingsHandler = initializer.getSettingsHandler();
        http2SettingsHandler.awaitSettings(60, TimeUnit.SECONDS);

        LOGGER.info("Sending request(s)...");
        final FullHttpRequest request = Http2Utils.createGetRequest(HOST, PORT);

        final Http2ClientResponseHandler responseHandler = initializer.getResponseHandler();
        final int streamId = 3;

        responseHandler.put(streamId, channel.write(request), channel.newPromise());
        channel.flush();

        final String response = responseHandler.awaitResponses(60, TimeUnit.SECONDS);

        assertEquals("Message", response);
        LOGGER.info("Finished HTTP/2 request(s)");
    }
}
