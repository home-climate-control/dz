package net.sf.dz3.view.webui.v1;

import net.sf.dz3.device.actuator.HvacController;
import net.sf.dz3.device.model.UnitSignal;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * Web UI for Home Climate Control.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class WebUI {

    protected final Logger logger = LogManager.getLogger();

    private final int port; // NOSONAR We'll get to it
    private final Set<Object> initSet = new HashSet<>(); // NOSONAR We'll get to it

    public WebUI(Set<Object> initSet) {
        this(3939, initSet);
    }

    public WebUI(int port, Set<Object> initSet) {

        this.port = port;
        this.initSet.addAll(initSet);

        logger.info("init set: {}", initSet);
    }

    public void activate() {

        var httpHandler = RouterFunctions.toHttpHandler(new RoutingConfiguration().monoRouterFunction(this));
        var adapter = new ReactorHttpHandlerAdapter(httpHandler);

        new Thread(() -> {
            var server = HttpServer.create().host("0.0.0.0").port(port);
            DisposableServer disposableServer = server.handle(adapter).bind().block();
            disposableServer.onDispose().block();
        }).start();

        logger.info("started");
    }

    /**
     * Response handler for the {@code /} HTTP request.
     *
     * @param rq Request object.
     *
     * @return Whole system representation.
     */
    public Mono<ServerResponse> getDashboard(ServerRequest rq) {

        // VT: NOTE: This is temporary; currently the system is merely a composition of zones,
        // but it's actually more than that.

        return ok().bodyValue("Oh, hai");
    }

    /**
     * Response handler for the zone set request.
     *
     * @param rq Request object.
     *
     * @return Set of zone representations.
     */
    public Mono<ServerResponse> getZones(ServerRequest rq) {
        return ok().render("zones");
    }

    /**
     * Response handler for individual zone request.
     *
     * @param rq Request object.
     *
     * @return Individual zone representation.
     */
    public Mono<ServerResponse> getZone(ServerRequest rq) {
        return ok().render("zone");
    }

    /**
     * Response handler for individual thermostat request.
     *
     * @param rq Request object.
     *
     * @return Individual thermostat representation
     */
    public Mono<ServerResponse> getThermostat(ServerRequest rq) {
        return ok().render("thermostat");
    }

    /**
     * Response handler for setting individual thermostat state.
     *
     * @param rq Request object.
     *
     * @return Command response.
     */
    public Mono<ServerResponse> setThermostat(ServerRequest rq) {
        return ok().render("thermostat");
    }

    /**
     * Response handler for the sensor set request.
     *
     * @param rq Request object.
     *
     * @return Set of sensor representations.
     */
    public Mono<ServerResponse> getSensors(ServerRequest rq) {

        var sensors = Flux.fromIterable(initSet)
                .filter(AnalogSensor.class::isInstance)
                .map(s -> new AnalogSensorSnapshot((AnalogSensor) s));

        return ok().contentType(MediaType.APPLICATION_JSON).body(sensors, AnalogSensor.class);
    }

    /**
     * Response handler for individual sensor request.
     *
     * @param rq Request object.
     *
     * @return Individual sensor representation.
     */
    public Mono<ServerResponse> getSensor(ServerRequest rq) {

        String address = rq.pathVariable("sensor");
        logger.info("/sensor/{}", address);

        var sensor = Flux.fromIterable(initSet)
                .filter(AnalogSensor.class::isInstance)
                .filter(s -> ((AnalogSensor) s).getAddress().equals(address))
                .map(s -> new AnalogSensorSnapshot((AnalogSensor) s));

        // Returning empty JSON is simpler on both receiving and sending side than a 404
        return ok().contentType(MediaType.APPLICATION_JSON).body(sensor, AnalogSensor.class);
    }

    /**
     * Response handler for the unit set request.
     *
     * @param rq Request object.
     *
     * @return Set of unit representations.
     */
    public Mono<ServerResponse> getUnits(ServerRequest rq) {

        var units = Flux.fromIterable(initSet)
                .filter(HvacController.class::isInstance)
                .map(c -> ((HvacController) c).getExtendedSignal());

        return ok().contentType(MediaType.APPLICATION_JSON).body(units, UnitSignal .class);
    }

    /**
     * Response handler for individual unit request.
     *
     * @param rq Request object.
     *
     * @return Individual unit representation.
     */
    public Mono<ServerResponse> getUnit(ServerRequest rq) {
        return ok().render("unit");
    }

    /**
     * Response handler for setting individual unit state.
     *
     * @param rq Request object.
     *
     * @return Command response.
     */
    public Mono<ServerResponse> setUnit(ServerRequest rq) {
        return ok().render("unit");
    }
}
