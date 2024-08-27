package net.sf.dz3r.view.webui.v2;

import com.homeclimatecontrol.hcc.Version;
import com.homeclimatecontrol.hcc.meta.EndpointMeta;
import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.runtime.InstanceIdProvider;
import net.sf.dz3r.runtime.config.model.TemperatureUnit;
import net.sf.dz3r.view.UnitObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.homeclimatecontrol.hcc.meta.EndpointMeta.Type.DIRECT;
import static net.sf.dz3r.view.webui.v2.RoutingConfiguration.META_PATH;

/**
 * Web UI for Home Climate Control - reactive version.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class WebUI implements AutoCloseable {

    private final Logger logger = LogManager.getLogger();

    public static final int DEFAULT_PORT_HTTP = 3939;
    public static final int DEFAULT_PORT_DUPLEX = 3940;

    private final Config config;
    private final Set<UnitDirector> initSet = new HashSet<>();

    private final Map<UnitDirector, UnitObserver> unit2observer = new TreeMap<>();

    private JmDNS jmDNS;

    public WebUI(
            String instance,
            String configDigest,
            int httpPort,
            int duplexPort,
            String interfaces,
            EndpointMeta endpointMeta,
            Set<UnitDirector> directors,
            InstrumentCluster ic,
            TemperatureUnit temperatureUnit) {

        this.config = new Config(instance, configDigest, httpPort, duplexPort, interfaces, endpointMeta, directors, ic, temperatureUnit);

        this.initSet.addAll(directors);

        logger.info("port: {}", httpPort);

        if (directors.isEmpty()) {
            logger.warn("empty init set, only diagnostic URLs will be available");
        } else {
            logger.info("init set: {}", directors);
        }

        Flux.just(Instant.now())
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::run);
    }

    private void run(Instant startedAt) {

        ThreadContext.push("run");

        try {

            // Run here instead of the constructor so that the control logic can start working sooner,
            // besides, if we fail here, it's a UI failure, and it will not disrupt the HVAC system operation.
            init();

            var httpEndpoint = new HttpEndpoint(
                    config.interfaces,
                    config.httpPort,
                    config.endpointMeta,
                    unit2observer);

            Flux.just(Instant.now())
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(httpEndpoint::run);

            var rsocketEndpoint = new RSocketEndpoint(
                    config.interfaces,
                    config.duplexPort);

            Flux.just(Instant.now())
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(rsocketEndpoint::run);

            advertise();

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
                new UnitDirectorInitializer().init(source);
            }
        } finally {
            ThreadContext.pop();
        }
    }

    private void advertise() {
        ThreadContext.push("mdns-advertise");
        try {

            var localhost = InetAddress.getLocalHost();
            var canonical = localhost.getCanonicalHostName();
            var fqdn = InetAddress.getByName(canonical);

            // Old bug: https://serverfault.com/questions/363095/why-does-my-hostname-appear-with-the-address-127-0-1-1-rather-than-127-0-0-1-in
            final var local11 = "127.0.1.1";

            if (fqdn.getHostAddress().equals(local11)) {
                logger.error("Check /etc/hosts for {}, it likely breaks mDNS resolution", local11);
                logger.error("More information: https://serverfault.com/questions/363095/why-does-my-hostname-appear-with-the-address-127-0-1-1-rather-than-127-0-0-1-in");
            }

            logger.debug("fqdn={}/{}", canonical, fqdn);

            jmDNS = JmDNS.create(fqdn);

            var propMap = Map.of(

                    "path", META_PATH, // http://www.dns-sd.org/txtrecords.html#http
                    "protocol-version", Version.PROTOCOL_VERSION,

                    "duplex-port", Integer.toString(config.duplexPort),
                    "type",DIRECT.toString().toLowerCase(),

                    "name", config.instance,
                    "vendor", "homeclimatecontrol.com",
                    "product", "hcc-core",

                    "unique-id", InstanceIdProvider.getId().toString(),
                    "config-digest", config.configDigest
            );

            var serviceInfo = ServiceInfo.create(
                    "_http._tcp.local.",
                    "HCC WebUI @" + config.instance,
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
            String instance,
            String configDigest,
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
