package net.sf.dz3r.view.webui.v2;

import com.homeclimatecontrol.hcc.ClientBootstrap;
import com.homeclimatecontrol.hcc.Version;
import com.homeclimatecontrol.hcc.meta.EndpointMeta;
import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;
import net.sf.dz3r.common.DurationFormatter;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.runtime.GitProperties;
import net.sf.dz3r.view.UnitObserver;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * HCC remote control endpoint over HTTP.
 *
 * This endpoint only serves small snapshots.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2024
 */
public class HttpServer extends Endpoint {

    private static final DurationFormatter uptimeFormatter = new DurationFormatter();

    private final String interfaces;
    private final int port;
    private final EndpointMeta endpointMeta;

    public HttpServer(String interfaces, int port, EndpointMeta endpointMeta, Map<UnitDirector, UnitObserver> unit2observer) {
        super(unit2observer);

        this.interfaces = interfaces;
        this.port = port;
        this.endpointMeta = endpointMeta;
    }

    void run(Instant startedAt) {

        try {

            var httpHandler = RouterFunctions.toHttpHandler(new RoutingConfiguration().monoRouterFunction(this));
            var adapter = new ReactorHttpHandlerAdapter(httpHandler);

            var server = reactor.netty.http.server.HttpServer.create().host(interfaces).port(port);
            var disposableServer = server.handle(adapter).bind().block();

            logger.info("started in {}ms", Duration.between(startedAt, Instant.now()).toMillis());


            disposableServer.onDispose().block(); // NOSONAR Acknowledged, ignored

            logger.info("done");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Advertise the capabilities ({@code HTTP GET /} request).
     *
     * @param ignoredRq ignored.
     *
     * @return Whole system representation ({@link EndpointMeta} as JSON).
     */
    public Mono<ServerResponse> getMeta(ServerRequest ignoredRq) {
        logger.info("GET ? " + Version.PROTOCOL_VERSION);

        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(endpointMeta), EndpointMeta.class);
    }

    /**
     * Response handler for the sensor set request.
     *
     * @param ignoredRq ignored.
     *
     * @return Set of sensor representations.
     */
    public Mono<ServerResponse> getSensors(ServerRequest ignoredRq) {
        logger.info("GET /sensors");

        return ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Flux.fromIterable(unit2observer.values())
                                .flatMap(UnitObserver::getSensors),
                        Signal.class);
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
        logger.info("GET /sensor/{}", address);

        return ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        Flux.fromIterable(unit2observer.values())
                                .flatMap(o -> o.getSensor(address)).next(),
                        Signal.class);
    }

    /**
     * Response handler for the unit set request.
     *
     * @param ignoredRq ignored.
     *
     * @return Set of unit representations.
     */
    public Mono<ServerResponse> getUnits(ServerRequest ignoredRq) {
        logger.info("GET /units");

        var units = Flux.fromIterable(unit2observer.entrySet())
                .map(kv -> new AbstractMap.SimpleEntry<>(
                        kv.getKey().getAddress(),
                        kv.getValue().getUnitStatus()))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);

        return ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_JSON)
                .body(units, Map.class);
    }

    /**
     * Response handler for individual unit request.
     *
     * @param rq Request object.
     *
     * @return Individual unit representation.
     */
    public Mono<ServerResponse> getUnit(ServerRequest rq) {

        String name = rq.pathVariable("unit");
        logger.info("GET /unit/{}", name);

        // Returning empty JSON is simpler on both receiving and sending side than a 404
        return ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Flux.fromIterable(unit2observer.entrySet())
                                .filter(kv -> kv.getKey().getAddress().equals(name))
                                .map(kv -> kv.getValue().getUnitStatus()),
                        Object.class);
    }

    /**
     * Response handler for setting individual unit state.
     *
     * @param rq Request object.
     *
     * @return Command response.
     */
    public Mono<ServerResponse> setUnit(ServerRequest rq) {
        String name = rq.pathVariable("unit");
        logger.info("POST /unit/{}", name);
        return ServerResponse.unprocessableEntity().bodyValue("Stay tuned, coming soon");
    }

    /**
     * Response handler for the zone set request.
     *
     * @param ignoredRq ignored.
     *
     * @return Set of zone representations.
     */
    public Mono<ServerResponse> getZones(ServerRequest ignoredRq) {
        logger.info("GET /zones");

        return ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Flux
                                .fromIterable(unit2observer.values())
                                .flatMap(UnitObserver::getZones)
                                .collectMap(Map.Entry::getKey, Map.Entry::getValue),
                        Map.class);
    }

    /**
     * Response handler for individual zone request.
     *
     * @param rq Request object.
     *
     * @return Individual zone representation.
     */
    public Mono<ServerResponse> getZone(ServerRequest rq) {

        String zone = rq.pathVariable("zone");
        logger.info("GET /zone/{}", zone);

        return ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        Flux.fromIterable(unit2observer.values())
                                .flatMap(o -> o.getZone(zone)).next(),
                        ZoneStatus.class);
    }

    /**
     * Response handler for setting individual zone state.
     *
     * @param rq Request object.
     *
     * @return Command response.
     */
    public Mono<ServerResponse> setZone(ServerRequest rq) {
        String zone = rq.pathVariable("zone");
        logger.info("POST /zone/{}", zone);
        return ServerResponse.unprocessableEntity().bodyValue("Stay tuned, coming soon");
    }

    /**
     * Get uptime.
     *
     * @param ignoredRq ignored.
     *
     * @return System uptime in both computer and human readable form.
     */
    public Mono<ServerResponse> getUptime(ServerRequest ignoredRq) {
        logger.info("GET /uptime");

        var mx = ManagementFactory.getRuntimeMXBean();
        var startMillis = mx.getStartTime();
        var uptimeMillis = mx.getUptime();
        var start = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date(startMillis)) + " " + ZoneId.systemDefault();
        var uptime = uptimeFormatter.format(uptimeMillis);

        // Let's make the JSON order predictable
        var result = new LinkedHashMap<>();

        result.put("start", start);
        result.put("uptime", uptime);
        result.put("start.millis", startMillis);
        result.put("uptime.millis", uptimeMillis);

        return ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Flux.fromIterable(result.entrySet()), Object.class);
    }

    /**
     * Get the version.
     *
     * @param ignoredRq ignored.
     *
     * @return Git revision properties.
     */
    public Mono<ServerResponse> getVersion(ServerRequest ignoredRq) {
        logger.info("GET /version");

        try {
            return ok().contentType(MediaType.APPLICATION_JSON).body(Flux.fromIterable(GitProperties.get().entrySet()), Object.class);
        } catch (IOException ex) {
            throw new IllegalStateException("This shouldn't have happened", ex);
        }
    }

    /**
     * Response handler for the bootstrap packet request.
     *
     * @param ignoredRq ignored.
     *
     * @return Bootstrap packet.
     */
    public Mono<ServerResponse> getBootstrap(ServerRequest ignoredRq) {
        logger.info("GET /bootstrap");

        var meta = Mono.just(endpointMeta);
        var zones = Flux
                .fromIterable(unit2observer.values())
                .flatMap(UnitObserver::getZones)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
        var combined = Mono
                .zip(meta, zones)
                .map(tuple -> new ClientBootstrap(tuple.getT1(), tuple.getT2()));

        return ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_JSON)
                .body(combined, ClientBootstrap.class);
    }
}
