package net.sf.dz3r.view.webui.v2;

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

    private static final RequestPredicate ACCEPT_JSON = accept(MediaType.APPLICATION_JSON);

    @Bean
    public RouterFunction<ServerResponse> monoRouterFunction(WebUI webUI) {
        return route(

                // Accessors

                GET("/").and(ACCEPT_JSON), webUI::getDashboard).andRoute(
                GET("/sensors").and(ACCEPT_JSON), webUI::getSensors).andRoute(
                GET("/sensor/{sensor}").and(ACCEPT_JSON), webUI::getSensor).andRoute(
                GET("/units").and(ACCEPT_JSON), webUI::getUnits).andRoute(
                GET("/unit/{unit}").and(ACCEPT_JSON), webUI::getUnit).andRoute(
                GET("/zones").and(ACCEPT_JSON), webUI::getZones).andRoute(
                GET("/zone/{zone}").and(ACCEPT_JSON), webUI::getZone).andRoute(

                GET("/version").and(ACCEPT_JSON), webUI::getVersion).andRoute(

                // Mutators

                POST("/zone{zone}").and(ACCEPT_JSON), webUI::setZone).andRoute(
                POST("/unit/{unit}").and(ACCEPT_JSON), webUI::setUnit);
    }
}
