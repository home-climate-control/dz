package net.sf.dz3.runtime.config.connector;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.http.gae.v3.HttpConnectorGAE;
import net.sf.dz3r.view.influxdb.v3.InfluxDbLogger;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ConnectorConfigurationParser extends ConfigurationContextAware {

    public ConnectorConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<ConnectorConfig> source) {

        for (var entry : source) {
            Optional.ofNullable(entry.http()).ifPresent(this::parseHttp);
            Optional.ofNullable(entry.influx()).ifPresent(this::parseInflux);
        }
    }

    private void parseHttp(HttpConnectorConfig cf) {

        try {

            context.connectors.register(cf.id(), new HttpConnectorGAE(new URL(cf.uri()), cf.zones()));

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
                        mapSensorFeed(cf.sensorFeedMapping())));
    }

    private Map<Flux<Signal<Double, Void>>, String> mapSensorFeed(Map<String, String> source) {

        logger.error("FIXME: mapSensorFeed() not implemented, empty InfluxDB feed for {}", source.keySet());

        return Map.of();
    }
}
