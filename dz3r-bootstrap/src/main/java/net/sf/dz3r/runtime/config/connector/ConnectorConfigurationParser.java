package net.sf.dz3r.runtime.config.connector;

import net.sf.dz3r.model.Zone;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
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
            Optional.ofNullable(entry.http()).ifPresent(this::parseHttp);
            Optional.ofNullable(entry.influx()).ifPresent(this::parseInflux);
        }
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
