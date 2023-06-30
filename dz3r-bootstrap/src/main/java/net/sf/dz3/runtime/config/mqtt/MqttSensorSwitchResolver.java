package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.ConfigurationMapper;
import net.sf.dz3.runtime.config.SensorSwitchResolver;
import net.sf.dz3.runtime.config.hardware.SensorConfig;
import net.sf.dz3.runtime.config.hardware.SwitchConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttBrokerSpec;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttGateway;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.signal.Signal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import reactor.core.publisher.Flux;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @param <A> Address type.
 * @param <L> Sensor adapter type.
 * @param <S> Switch adapter type.
 */
public abstract class MqttSensorSwitchResolver<A extends MqttGateway, L, S> extends SensorSwitchResolver<A> {

    private final Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter;
    private final Set<MqttSensorConfig> sensorConfigs = new LinkedHashSet<>();
    private final Set<MqttSwitchConfig> switchConfigs = new LinkedHashSet<>();

    protected MqttSensorSwitchResolver(Set<A> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source);
        this.endpoint2adapter = endpoint2adapter;
    }

    @Override
    public final Map<String, Flux<Signal<Double, Void>>> getSensorFluxes() {

        return getSensorFluxes(endpoint2adapter, sensorConfigs);
    }

    protected abstract Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter, Set<MqttSensorConfig> source);

    public final Flux<MqttEndpointSpec> getEndpoints() {

        var endpoints = new LinkedHashSet<MqttEndpointSpec>();

        return Flux.create(sink -> {

            var sequence = Flux
                    .fromIterable(source)

                    .map(item -> new ImmutablePair<>(ConfigurationMapper.INSTANCE.parseEndpoint(item), item))
                    .subscribe(kv -> {
                        var key = kv.getKey();
                        if (!endpoints.contains(key)) {
                            sink.next(ConfigurationMapper.INSTANCE.parseEndpoint(kv.getValue()));
                            endpoints.add(key);
                        }
                    });

            sink.complete();
            sequence.dispose();
        });
    }

    /**
     * Parse the configuration into the mapping from their broker (not endpoint) to sensor configuration.
     */
    public final void getSensorConfigs() {

        Flux
                .fromIterable(source)
                .subscribe(s -> {
                    var endpoint = ConfigurationMapper.INSTANCE.parseBroker(s);
                    Optional.ofNullable(s.sensors()).ifPresent(sensors -> {
                        for (var spec : s.sensors()) {
                            sensorConfigs.add(new MqttSensorConfig(endpoint, spec));
                        }
                    });
                });
    }

    /**
     * Parse the configuration into the mapping from their broker (not endpoint) to switch configuration.
     */
    public final void getSwitchConfigs() {

        Flux
                .fromIterable(source)
                .subscribe(s -> {
                    var endpoint = ConfigurationMapper.INSTANCE.parseBroker(s);
                    Optional.ofNullable(s.sensors()).ifPresent(sensors -> {
                        for (var spec : s.switches()) {
                            switchConfigs.add(new MqttSwitchConfig(endpoint, spec));
                        }
                    });
                });
    }

    protected abstract L createSensorListener(MqttAdapter adapter, String rootTopic);
    public record MqttSensorConfig(
            MqttBrokerSpec mqttBrokerSpec,
            SensorConfig sensorConfig) {

    }

    public record MqttSwitchConfig(
            MqttBrokerSpec mqttBrokerSpec,
            SwitchConfig switchConfig) {

    }
}
