package net.sf.dz3r.view.webui.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.ByteBufPayload;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.view.UnitObserver;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

/**
 * HCC remote control endpoint over RSocket.
 *
 * This endpoint only serves streams, and big snapshots.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2024
 */
public class RSocketServer extends Endpoint {

    public enum Command {
        ZONES,
        SENSORS,
        UNITS
    }

    private final String interfaces;
    private final int port;

    private final ProtocolHandler protocolHandler = new ProtocolHandler();

    public RSocketServer(String interfaces, int port, Map<UnitDirector, UnitObserver> unit2observer) {
        super(unit2observer);

        this.interfaces = interfaces;
        this.port = port;
    }

    void run(Instant startedAt) {

        var closeableChannel = io.rsocket.core.RSocketServer
                .create(SocketAcceptor.with(protocolHandler))
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .bind(TcpServerTransport.create(interfaces, port))
                .block();

        logger.info("started in {}ms", Duration.between(startedAt, Instant.now()).toMillis());

        // VT: FIXME: This behaves differently from HttpServer; need to make sure shutdown is properly handled

        closeableChannel.onClose().block();

        logger.info("done");
    }

    private class ProtocolHandler implements RSocket {

        @Override
        public Mono<Payload> requestResponse(Payload payload) {
            ThreadContext.push("requestResponse");

            try {

                // VT: FIXME: Revisit https://github.com/rsocket/rsocket/blob/master/Extensions/Routing.md to improve this

                var route = decodeRoute(payload);

                // At this point, route is just the serialization algorithm, and only JSON is supported

                if (!"JSON".equals(route)) {
                    return Mono.error(new IllegalArgumentException("Unknown route '" + route + "', only JSON is supported"));
                }

                var commandText = payload.getDataUtf8();

                logger.debug("command/text: {}", commandText);

                var command = Command.valueOf(commandText.toUpperCase());

                logger.info("command/parsed: {}", command);

                // VT: FIXME: Create pluggable serializers to avoid ugly if/then/else
                return switch (command) {
                    case ZONES -> getZones(route);
                    case SENSORS, UNITS -> Mono.error(new IllegalArgumentException("Don't know how to process '" + command + "'"));
                };

            } finally {
                payload.release();
                ThreadContext.pop();
            }
        }

        private Mono<Payload> getZones(String serialization) {

            return Flux.fromIterable(unit2observer.values())
                    .flatMap(UnitObserver::getZones)
                    .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                    .map(o -> {
                        try {
                            return ByteBufPayload.create(objectMapper.writeValueAsString(o));
                        } catch (JsonProcessingException ex) {
                            logger.error("Can't serialize object, exception will pop up at the client side: {}", o, ex);
                            return ex;
                        }
                    }).flatMap(this::processResponse);
        }

        private Mono<Payload> processResponse(Object source) {

            if (source instanceof Payload payload) {
                return Mono.just(payload);
            }

            return Mono.error((Throwable) source);
        }

        private String decodeRoute(Payload payload) {

            if (!payload.hasMetadata()) {
                logger.warn("no metadata");
                return null;
            }

            var routingMeta = new RoutingMetadata(payload.sliceMetadata());
            var items = new ArrayList<String>();

            Flux
                    // routingMeta can only be iterated once
                    .fromIterable(routingMeta)
                    .doOnNext(items::add)
                    .subscribe(r -> logger.info("routing metadata element: {}", r));

            if (items.isEmpty()) {
                logger.warn("routing metadata didn't parse correctly");
                return null;
            }

            logger.info("route: {}", items.get(0));

            return items.get(0);
        }
    }
}
