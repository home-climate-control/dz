package net.sf.dz3r.view.webui.v2;

import com.homeclimatecontrol.hcc.Version;
import com.homeclimatecontrol.hcc.meta.EndpointMeta;
import net.sf.dz3r.common.DurationFormatter;
import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.runtime.GitProperties;
import net.sf.dz3r.runtime.config.model.TemperatureUnit;
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

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static net.sf.dz3r.view.webui.v2.RoutingConfiguration.META_PATH;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * Web UI for Home Climate Control - reactive version.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class WebUI implements AutoCloseable {

    protected final Logger logger = LogManager.getLogger();

    public static final int DEFAULT_PORT_HTTP = 3939;
    public static final int DEFAULT_PORT_DUPLEX = 3940;

    private final Config config;
    private final Set<UnitDirector> initSet = new HashSet<>();

    private final Map<UnitDirector, UnitObserver> unit2observer = new TreeMap<>();

    private JmDNS jmDNS;

    public WebUI(int httpPort,
                 int duplexPort,
                 String interfaces,
                 EndpointMeta endpointMeta,
                 Set<UnitDirector> directors,
                 InstrumentCluster ic,
                 TemperatureUnit temperatureUnit) {

        this.config = new Config(httpPort, duplexPort, interfaces, endpointMeta, directors, ic, temperatureUnit);

        this.initSet.addAll(directors);

        logger.info("port: {}", httpPort);

        if (directors.isEmpty()) {
            logger.warn("empty init set, only diagnostic URLs will be available");
        } else {
            logger.info("init set: {}", directors);
        }

        Flux.just(Instant.now())
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::run)
                .subscribe();
    }

    private void run(Instant startedAt) {

        ThreadContext.push("run");

        try {

            // Run here instead of the constructor so that the control logic can start working sooner,
            // besides, if we fail here, it's a UI failure, and it will not disrupt the HVAC system operation.
            init();

            var httpHandler = RouterFunctions.toHttpHandler(new RoutingConfiguration().monoRouterFunction(this));
            var adapter = new ReactorHttpHandlerAdapter(httpHandler);

            var server = HttpServer.create().host(config.interfaces).port(config.httpPort);
            DisposableServer disposableServer = server.handle(adapter).bind().block();

            logger.info("started in {}ms", Duration.between(startedAt, Instant.now()).toMillis());

            advertise();

            disposableServer.onDispose().block(); // NOSONAR Acknowledged, ignored

            logger.info("done");

        } finally {
            ThreadContext.pop();
        }
    }

    private void advertise() {
        ThreadContext.push("mdns-advertise");
        try {

            var localhost = InetAddress.getLocalHost();
            var name = localhost.getHostName();
            var canonical = localhost.getCanonicalHostName();
            var fqdn = InetAddress.getByName(canonical);

            // Old bug: https://serverfault.com/questions/363095/why-does-my-hostname-appear-with-the-address-127-0-1-1-rather-than-127-0-0-1-in
            final var local11 = "127.0.1.1";

            if (fqdn.getHostAddress().equals(local11)) {
                logger.error("Check /etc/hosts for {}, it likely breaks mDNS resolution", local11);
            }

            logger.debug("fqdn={}/{}", canonical, fqdn);

            jmDNS = JmDNS.create(fqdn);

            var propMap = Map.of(
                    "path", META_PATH, // http://www.dns-sd.org/txtrecords.html#http
                    "protocol-version", Version.PROTOCOL_VERSION,
                    "duplex", Integer.toString(config.duplexPort)
            );

            var serviceInfo = ServiceInfo.create(
                    "_http._tcp.local.",
                    "HCC WebUI @" + name,
                    "",
                    config.httpPort,
                    0,
                    0,
                    propMap
            );

            jmDNS.registerService(serviceInfo);

            logger.debug("mDNS registered: {}", serviceInfo);


        } catch (IOException ex) {
            logger.error("failed to advertise over mDNS, ignored", ex);
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
                new UnitDirectorInitializer().init(source);
            }
        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Advertise the capabilities ({@code HTTP GET /} request).
     *
     * @param rq ignored.
     *
     * @return Whole system representation ({@link EndpointMeta} as JSON).
     */
    public Mono<ServerResponse> getMeta(ServerRequest rq) {
        logger.info("GET ? " + Version.PROTOCOL_VERSION);

        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Flux.just(config.endpointMeta), EndpointMeta.class);
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

    private static final DurationFormatter uptimeFormatter = new DurationFormatter();
    public Mono<ServerResponse> getUptime(ServerRequest rq) {
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

        return ok().contentType(MediaType.APPLICATION_JSON).body(Flux.fromIterable(result.entrySet()), Object.class);
    }

    public Mono<ServerResponse> getVersion(ServerRequest rq) {
        logger.info("GET /version");

        try {
            return ok().contentType(MediaType.APPLICATION_JSON).body(Flux.fromIterable(GitProperties.get().entrySet()), Object.class);
        } catch (IOException ex) {
            throw new IllegalStateException("This shouldn't have happened", ex);
        }
    }

    @Override
    public void close() throws IOException {
        if (jmDNS != null) {
            jmDNS.unregisterAllServices();
            jmDNS.close();
        }
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
    public record Config(
            int httpPort,
            int duplexPort,
            String interfaces,
            EndpointMeta endpointMeta,
            Set<UnitDirector> directors,
            InstrumentCluster ic,
            TemperatureUnit initialUnit
    ) {

    }
}
