package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.ConfigurationMapper;
import net.sf.dz3.runtime.config.SensorSwitchResolver;
import net.sf.dz3.runtime.config.hardware.SensorConfig;
import net.sf.dz3.runtime.config.hardware.SwitchConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttBrokerSpec;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttGateway;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class MqttSensorSwitchResolver<T extends MqttGateway> extends SensorSwitchResolver<T> {

    protected final Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter;

    protected MqttSensorSwitchResolver(Set<T> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source);

        this.endpoint2adapter = endpoint2adapter;
    }

    public final Flux<MqttEndpointSpec> getEndpoints() {

        var endpoints = new LinkedHashSet<String>();

        return Flux.create(sink -> {

            var sequence = Flux
                    .fromIterable(source)

                    .map(item -> new ImmutablePair<>(item.signature(), item))
                    .doOnNext(kv -> {

                        var key = kv.getKey();
                        if (!endpoints.contains(key)) {
                            sink.next(ConfigurationMapper.INSTANCE.parseEndpoint(kv.getValue()));
                            endpoints.add(key);
                        }
                    })
                    .subscribe();

            sink.complete();
            sequence.dispose();
        });
    }

    /**
     * Parse the configuration into the mapping from their broker (not endpoint) to sensor configuration.
     *
     * @return Map of (broker spec, sensor configuration) for all the given sources.
     */
    public final Flux<Pair<MqttBrokerSpec, SensorConfig>> getSensorConfigs() {

        return Flux.create(sink -> {
            var sequence = Flux
                    .fromIterable(source)
                    .doOnNext(s -> {
                        var endpoint = ConfigurationMapper.INSTANCE.parseBroker(s);
                        Optional.ofNullable(s.sensors()).ifPresent(sensors -> {
                            for (var spec : s.sensors()) {
                                sink.next(new ImmutablePair<>(endpoint, spec));
                            }
                        });
                    })
                    .subscribe();

            sink.complete();
            sequence.dispose();
        });
    }

    /**
     * Parse the configuration into the mapping from their broker (not endpoint) to switch configuration.
     *
     * @return Map of (broker spec, switch configuration) for all the given sources.
     */
    public final Flux<Pair<MqttBrokerSpec, SwitchConfig>> getSwitchConfigs() {

        return Flux.create(sink -> {
            var sequence = Flux
                    .fromIterable(source)
                    .doOnNext(s -> {
                        var endpoint = ConfigurationMapper.INSTANCE.parseBroker(s);
                        Optional.ofNullable(s.switches()).ifPresent(sensors -> {
                            for (var spec : s.switches()) {
                                sink.next(new ImmutablePair<>(endpoint, spec));
                            }
                        });
                    })
                    .subscribe();

            sink.complete();
            sequence.dispose();
        });
    }
}
