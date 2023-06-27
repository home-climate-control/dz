package net.sf.dz3.runtime.metrics.prometheus;

import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class RoutingConfiguration {

    @Bean
    public RouterFunction<ServerResponse> monoRouterFunction(Endpoint endpoint) {
        return route(
                GET("/metrics"), endpoint::scrape);
    }
}
