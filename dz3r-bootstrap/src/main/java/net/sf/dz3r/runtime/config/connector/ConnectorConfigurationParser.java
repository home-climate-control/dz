package net.sf.dz3r.runtime.config.connector;

import net.sf.dz3r.model.Zone;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttBrokerConfig;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.http.gae.v3.HttpConnectorGAE;
import net.sf.dz3r.view.influxdb.v3.InfluxDbLogger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ConnectorConfigurationParser extends ConfigurationContextAware {

    public ConnectorConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<ConnectorConfig> source) {

        for (var entry : Optional.ofNullable(source).orElse(Set.of())) {
            Optional.ofNullable(entry.homeAssistant()).ifPresent(this::parseHomeAssistant);
            Optional.ofNullable(entry.http()).ifPresent(this::parseHttp);
            Optional.ofNullable(entry.influx()).ifPresent(this::parseInflux);
        }
    }

    private void parseHomeAssistant(HomeAssistantConfig cf) {

        // Some sanity checking first

        // VT: NOTE: With existing interfaces and records, either Quarkus will choke on missing root topic,
        // or I will spend inordinate time refactoring it. So, Worse is Better.
        // Just require one of (broker.root-topic, discovery-prefix) to be present.

        if (cf.broker().rootTopic() != null && cf.discoveryPrefix() != null) {
            throw new IllegalArgumentException("both broker.root-topic and discovery-prefix are present, must specify only one");
        }

        var topic = Optional.ofNullable(cf.broker().rootTopic()).orElse(cf.discoveryPrefix());
        var id = Optional.ofNullable(cf.broker().id()).orElse(Integer.toHexString((cf.broker().host() + ":" + cf.broker().port()).hashCode()));

        var broker = new MqttBrokerConfig(
                id,
                cf.broker().host(),
                cf.broker().port(),
                cf.broker().username(),
                cf.broker().password(),
                topic,
                cf.broker().autoReconnect()
        );

//        context.connectors.register(broker.signature(), new HomeAssistantConnector());
    }

    private void parseHttp(HttpConnectorConfig cf) {

        try {

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
        }
    }

    private void parseInflux(InfluxCollectorConfig cf) {

        context.collectors.register(
                cf.id(),
                new InfluxDbLogger(
                        cf.db(),
                        cf.instance(),
                        cf.uri(),
                        cf.username(),
                        cf.password(),
                        getSensorFeed2IdMapping(cf.sensorFeedMapping())));
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
