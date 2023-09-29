package net.sf.dz3r.runtime.config.connector;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.runtime.GitProperties;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.runtime.config.ConfigurationMapper;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.ha.HomeAssistantConnector;
import net.sf.dz3r.view.http.gae.v3.HttpConnectorGAE;
import net.sf.dz3r.view.influxdb.v3.InfluxDbLogger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ConnectorConfigurationParser extends ConfigurationContextAware {

    private final String softwareVersion;

    public ConnectorConfigurationParser(ConfigurationContext context) throws IOException {
        super(context);

        var p = GitProperties.get();
        var branch = p.get("git.branch");
        var rev = p.get("git.commit.id.abbrev");

        softwareVersion = branch + "-" + rev;
    }

    public Flux<Object> parse(Set<ConnectorConfig> source) {

        Marker m = new Marker("parse connectors");
        var flux = Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(entry -> Mono.create(sink -> {

                        Optional.ofNullable(entry.homeAssistant()).ifPresent(this::parseHomeAssistant);
                        Optional.ofNullable(entry.http()).ifPresent(this::parseHttp);
                        Optional.ofNullable(entry.influx()).ifPresent(this::parseInflux);

                        sink.success("done: " + entry.toString());
                    }
                ))
                .sequential()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(ignore -> logger.debug("parse connector: {}", ignore))
                .doOnComplete(m::close)

                // Prevent multiple subscriptions
                .publish()
                .autoConnect();

        // Start right away
        flux.subscribe(e -> logger.debug("subscription: {}", e));

        return flux;
    }

    /**
     * Parse HA configuration and register the instance.
     *
     * @param cf HA Configuration
     */
    private void parseHomeAssistant(HomeAssistantConfig cf) {

        Marker m = new Marker("parseHA");

        try {
            HCCObjects.requireNonNull(cf.id(), "connectors.ha.id is missing");
            var zonesExposed = Optional.ofNullable(cf.zones()).orElse(Set.of());

            if (zonesExposed.isEmpty()) {
                logger.warn("{}: no zones specified, all will be exposed, make sure this is what you want", cf.id());
            }

            var brokerConfig = cf.parse();
            var mqttAdapterSignature = ConfigurationMapper.INSTANCE.parseEndpoint(brokerConfig).signature();
            var mqttAdapterConfigured = new TreeSet<String>();
            var mqttAdapter = context.mqtt
                    .getFlux()
                    .doOnNext(entry -> mqttAdapterConfigured.add(entry.getKey()))
                    .filter(entry -> entry.getKey().equals(mqttAdapterSignature))
                    .map(Map.Entry::getValue)
                    .blockFirst();

            if (mqttAdapter == null) {
                logger.error("{}: couldn't find an adapter with signature {}, this can't be happening. Existing adapters ({} of them):", cf.id(), mqttAdapterSignature, mqttAdapterConfigured.size());
                for (var signature : mqttAdapterConfigured) {
                    logger.error("  {}", signature);
                }
                throw new IllegalStateException("This should not be happening - check the logs at error level");
            }

            var zones = context.zones
                    .getFlux()
                    .doOnNext(kv -> logger.debug("{}: zone configured: {}", cf.id(), kv.getKey()))
                    .filter(kv -> zonesExposed.isEmpty() || zonesExposed.contains(kv.getKey()))
                    .doOnNext(kv -> logger.debug("{}: zone exposed: {}", cf.id(), kv.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet())
                    .block();

            context.connectors.register(brokerConfig.signature(), new HomeAssistantConnector(softwareVersion, cf.id(), mqttAdapter, brokerConfig.rootTopic(), zones));

        } finally {
            m.close();
        }
    }

    private void parseHttp(HttpConnectorConfig cf) {

        Marker m = new Marker("parseHttp");
        try {
            HCCObjects.requireNonNull(cf.id(), "connectors.http.id is missing");

            // Configuration contains IDs, connector doesn't know and doesn't care
            var zones = Flux
                    .fromIterable(cf.zones())
                    .flatMap(name -> context.zones.getMonoById("connectors.http", name))
                    .map(Zone::getAddress)
                    .collect(Collectors.toSet())
                    .block();

            if (zones == null || zones.isEmpty()) {
                logger.warn("connectors.http.{}: no reportable zones found, not creating the connector", cf.id());
                return;
            }

            context.connectors.register(cf.id(), new HttpConnectorGAE(new URL(cf.uri()), zones));

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: '" + cf.uri() + "'");
        } finally {
            m.close();
        }
    }

    private void parseInflux(InfluxCollectorConfig cf) {

        Marker m = new Marker("parseInflux");
        try {
            HCCObjects.requireNonNull(cf.id(), "connectors.influx.id is missing");
            context.collectors.register(
                    cf.id(),
                    new InfluxDbLogger(
                            cf.db(),
                            cf.instance(),
                            cf.uri(),
                            cf.username(),
                            cf.password(),
                            getSensorFeed2IdMapping(cf.sensorFeedMapping())));
        } finally {
            m.close();
        }
    }

    /**
     * Unlike {@link #getSensorFeed2ZoneMapping(Map)}, this returns the mapping from the feed
     * to simply the ID it will be reported as.
     *
     * @param source Mapping from the sensor feed ID to "reported as" ID.
     */
    private Map<Flux<Signal<Double, Void>>, String> getSensorFeed2IdMapping(Map<String, String> source) {

        return Flux
                .fromIterable(source.entrySet())
                .map(kv -> new ImmutablePair<>(
                        getSensorBlocking(kv.getKey()),
                        kv.getValue()
                ))
                .collectMap(Pair::getKey, Pair::getValue)
                .block();
    }
}
