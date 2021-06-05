package net.sf.dz3.view.webui.v1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration(proxyBeanMethods = false)
public class RoutingConfiguration {

    private static final RequestPredicate ACCEPT_JSON = accept(MediaType.APPLICATION_JSON);

    @Bean
    public RouterFunction<ServerResponse> monoRouterFunction(WebUI webUI) {
        return route(
                GET("/").and(ACCEPT_JSON), webUI::getDashboard).andRoute(
                GET("/zones").and(ACCEPT_JSON), webUI::getZones).andRoute(
                GET("/{zone}").and(ACCEPT_JSON), webUI::getZone).andRoute(
                GET("/{thermostat}").and(ACCEPT_JSON), webUI::getThermostat).andRoute(
                POST("/{thermostat}").and(ACCEPT_JSON), webUI::setThermostat).andRoute(
                GET("/sensors").and(ACCEPT_JSON), webUI::getSensors).andRoute(
                GET("/sensor/{sensor}").and(ACCEPT_JSON), webUI::getSensor).andRoute(
                GET("/units").and(ACCEPT_JSON), webUI::getUnits).andRoute(
                GET("/unit/{unit}").and(ACCEPT_JSON), webUI::getUnit).andRoute(
                POST("/unit/{unit}").and(ACCEPT_JSON), webUI::setUnit);
    }
}
