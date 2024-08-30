package net.sf.dz3r.view.webui.v2;

import com.homeclimatecontrol.hcc.Version;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class RoutingConfiguration {

    public static final String META_PATH = "/meta/" + Version.PROTOCOL_VERSION;
    private static final RequestPredicate ACCEPT_JSON = accept(MediaType.APPLICATION_JSON);

    @Bean
    public RouterFunction<ServerResponse> monoRouterFunction(HttpServer endpoint) {
        return route(

                // Accessors

                GET("/").and(ACCEPT_JSON), endpoint::getMeta).andRoute(
                GET(META_PATH).and(ACCEPT_JSON), endpoint::getMeta).andRoute(
                GET("/sensors").and(ACCEPT_JSON), endpoint::getSensors).andRoute(
                GET("/sensor/{sensor}").and(ACCEPT_JSON), endpoint::getSensor).andRoute(
                GET("/units").and(ACCEPT_JSON), endpoint::getUnits).andRoute(
                GET("/unit/{unit}").and(ACCEPT_JSON), endpoint::getUnit).andRoute(
                GET("/zones").and(ACCEPT_JSON), endpoint::getZones).andRoute(
                GET("/zone/{zone}").and(ACCEPT_JSON), endpoint::getZone).andRoute(

                GET("/uptime").and(ACCEPT_JSON), endpoint::getUptime).andRoute(
                GET("/version").and(ACCEPT_JSON), endpoint::getVersion).andRoute(

                // Mutators

                POST("/zone{zone}").and(ACCEPT_JSON), endpoint::setZone).andRoute(
                POST("/unit/{unit}").and(ACCEPT_JSON), endpoint::setUnit);
    }
}
