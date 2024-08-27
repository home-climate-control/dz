package net.sf.dz3r.view.webui.v2;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.ByteBufPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

public class RSocketEndpoint {

    private final Logger logger = LogManager.getLogger();

    private final String interfaces;
    private final int port;

    private final ProtocolHandler protocolHandler = new ProtocolHandler();

    public RSocketEndpoint(String interfaces, int port) {
        this.interfaces = interfaces;
        this.port = port;
    }

    void run(Instant startedAt) {

        var closeableChannel = RSocketServer
                .create(SocketAcceptor.with(protocolHandler))
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .bind(TcpServerTransport.create(interfaces, port))
                .block();

        logger.info("started in {}ms", Duration.between(startedAt, Instant.now()).toMillis());

        // VT: FIXME: This behaves differently from HttpEndpoint; need to make sure shutdown is properly handled

        closeableChannel.onClose().block();

        logger.info("done");
    }

    private class ProtocolHandler implements RSocket {

        @Override
        public Mono<Payload> requestResponse(Payload payload) {
            ThreadContext.push("requestResponse");

            try {

                var route = decodeRoute(payload);
                logger.info("route: {}", route);

                return Mono.just(ByteBufPayload.create("now: " + Instant.now()));

            } finally {
                payload.release();
                ThreadContext.pop();
            }
        }

        private String decodeRoute(Payload payload) {

            if (!payload.hasMetadata()) {
                logger.info("routing metadata missing");
                return null;
            }

            var routingMeta = new RoutingMetadata(payload.sliceMetadata());

            logger.info("routing metadata: {}", routingMeta);

            return routingMeta.iterator().next();
        }
    }
}
