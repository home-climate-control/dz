package net.sf.dz3r.view.webui.v2;

import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import net.sf.dz3r.view.UnitObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * Web UI for Home Climate Control - reactive version.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class WebUI {

    protected final Logger logger = LogManager.getLogger();

    private final int port; // NOSONAR We'll get to it
    private final Set<Object> initSet = new HashSet<>(); // NOSONAR We'll get to it

    private final Map<UnitDirector, UnitObserver> unit2observer = new TreeMap<>();

    public WebUI(Set<Object> initSet) {
        this(3939, initSet);
    }

    public WebUI(int port, Set<Object> initSet) {

        this.port = port;
        this.initSet.addAll(initSet);

        logger.info("init set: {}", initSet);

        Flux.just(Instant.now())
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::start)
                .subscribe();
    }

    private void start(Instant startedAt) {

        ThreadContext.push("start");

        try {

            // Run here instead of the constructor so that the control logic can start working sooner,
            // besides, if we fail here, it's a UI failure, and it will not disrupt the HVAC system operation.
            init();

            var httpHandler = RouterFunctions.toHttpHandler(new RoutingConfiguration().monoRouterFunction(this));
            var adapter = new ReactorHttpHandlerAdapter(httpHandler);

            var server = HttpServer.create().host("0.0.0.0").port(port);
            DisposableServer disposableServer = server.handle(adapter).bind().block();

            logger.info("started in {}ms", Duration.between(startedAt, Instant.now()).toMillis());

            disposableServer.onDispose().block();

            logger.info("done");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Initialize everything from the given set of init objects.
     */
    private void init() {

        ThreadContext.push("init");
        try {

            for (var source : initSet) {

                switch (source.getClass().getSimpleName()) { // NOSONAR More coming
                    case "UnitDirector":
                        new UnitDirectorInitializer().init((UnitDirector) source);
                        break;
                    default:
                        logger.warn("Don't know how to handle {}", source.getClass().getName());
                }
            }
        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Response handler for the {@code /} HTTP request.
     *
     * @param rq Request object.
     *
     * @return Whole system representation.
     */
    public Mono<ServerResponse> getDashboard(ServerRequest rq) {
        return ServerResponse.unprocessableEntity().bodyValue("Stay tuned, coming soon");
    }

    /**
     * Response handler for the zone set request.
     *
     * @param rq Request object.
     *
     * @return Set of zone representations.
     */
    public Mono<ServerResponse> getZones(ServerRequest rq) {
        logger.info("GET /zones");

        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Flux.fromIterable(unit2observer.values())
                                .flatMap(UnitObserver::getZones),
                        ZoneStatus.class);
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
     * Response handler for the sensor set request.
     *
     * @param rq Request object.
     *
     * @return Set of sensor representations.
     */
    public Mono<ServerResponse> getSensors(ServerRequest rq) {
        logger.info("GET /sensors");

        return ok()
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
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        Flux.fromIterable(unit2observer.values())
                                .flatMap(o -> o.getSensor(address)).next(),
                        Signal.class);
    }

    /**
     * Response handler for the unit set request.
     *
     * @param rq Request object.
     *
     * @return Set of unit representations.
     */
    public Mono<ServerResponse> getUnits(ServerRequest rq) {
        logger.info("GET /units");

        var units = Flux.fromIterable(unit2observer.entrySet())
                .map(kv -> new AbstractMap.SimpleEntry<>(
                        kv.getKey().getAddress(),
                        kv.getValue().getUnitStatus()));

        return ok().contentType(MediaType.APPLICATION_JSON).body(units, Map.class);
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

    private interface Initializer<T> {
        void init(T source);
    }

    private class UnitDirectorInitializer implements Initializer<UnitDirector> {

        @Override
        public void init(UnitDirector source) {

            logger.info("UnitDirector: {}", source);
            var observer = new UnitObserver(source);
            unit2observer.put(source, observer);
        }
    }
}
