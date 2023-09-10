package net.sf.dz3r.runtime.metrics.prometheus;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

public class Endpoint {

    private final PrometheusMeterRegistry registry;

    public Endpoint(PrometheusMeterRegistry registry) {

        this.registry = registry;

        Flux.just(Instant.now())
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::start)
                .subscribe();
    }

    private void start(Instant instant) {

        ThreadContext.push("start");

        try {

            var httpHandler = RouterFunctions.toHttpHandler(new RoutingConfiguration().monoRouterFunction(this));
            var adapter = new ReactorHttpHandlerAdapter(httpHandler);

            var server = HttpServer.create().host("0.0.0.0").port(3940);
            DisposableServer disposableServer = server.handle(adapter).bind().block();

            disposableServer.onDispose().block(); // NOSONAR Acknowledged, ignored

        } finally {
            ThreadContext.pop();
        }
    }

    public Mono<ServerResponse> scrape(ServerRequest rq) {

        StringWriter sw = new StringWriter();

        try {

            registry.scrape(sw);
            return ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(BodyInserters.fromValue(sw.toString()));

        } catch (IOException e) {
            return Mono.error(e);
        }
    }
}
