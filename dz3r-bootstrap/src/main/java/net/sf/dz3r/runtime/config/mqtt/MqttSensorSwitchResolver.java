package net.sf.dz3r.runtime.config.mqtt;

import net.sf.dz3r.device.mqtt.v1.AbstractMqttSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.runtime.config.ConfigurationMapper;
import net.sf.dz3r.runtime.config.SensorSwitchResolver;
import net.sf.dz3r.runtime.config.hardware.SensorConfig;
import net.sf.dz3r.runtime.config.hardware.SwitchConfig;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttBrokerSpec;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttGateway;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import net.sf.dz3r.signal.filter.TimeoutGuard;
import org.apache.commons.lang3.tuple.ImmutablePair;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Common implementations for resolving MQTT sensor and switch instances from their configurations.
 *
 * @param <A> Address type.
 * @param <L> Sensor adapter type.
 * @param <S> Switch adapter type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class MqttSensorSwitchResolver<A extends MqttGateway, L extends SignalSource<String, Double, Void>, S extends AbstractMqttSwitch> extends SensorSwitchResolver<A> {

    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter;
    private final Set<MqttSensorConfig> sensorConfigs = new LinkedHashSet<>();
    private final Set<MqttSwitchConfig> switchConfigs = new LinkedHashSet<>();

    private final Map<MqttBrokerSpec, L> address2sensor = new LinkedHashMap<>();

    protected MqttSensorSwitchResolver(Set<A> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source);
        this.endpoint2adapter = endpoint2adapter;
    }

    @Override
    public final Flux<Map.Entry<String, Flux<Signal<Double, Void>>>> getSensorFluxes() {
        return getSensorFluxes(endpoint2adapter, sensorConfigs);
    }

    private Flux<Map.Entry<String, Flux<Signal<Double, Void>>>> getSensorFluxes(Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter, Set<MqttSensorConfig> source) {

        return Flux
                .fromIterable(source)
                .doOnNext(c -> logger.debug("sensor: {}", c.sensorConfig().address()))
                .doOnNext(c -> logger.debug("  broker: {}", c.mqttBrokerSpec().signature()))
                .doOnNext(c -> logger.debug("  endpoint: {}", endpoint2adapter.get(ConfigurationMapper.INSTANCE.parseEndpoint(c.mqttBrokerSpec())).address))
                .map(c -> {
                    var adapter = endpoint2adapter.get(ConfigurationMapper.INSTANCE.parseEndpoint(c.mqttBrokerSpec()));
                    var listener = resolveListener(c.mqttBrokerSpec(), adapter);

                    return new ImmutablePair<>(c, listener);
                })

                // This is where things get hairy
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .map(kv -> {
                    var id = kv.getKey().sensorConfig().id();
                    var address = kv.getKey().sensorConfig().address();
                    var flux = kv.getValue().getFlux(address);
                    var timeout = Optional
                            .ofNullable(kv.getKey().sensorConfig.timeout())
                            .orElseGet(() -> {
                                logger.warn("{}: default timeout of {} is used", kv.getKey().sensorConfig(), DEFAULT_TIMEOUT);
                                return DEFAULT_TIMEOUT;
                            });
                    var guard = new TimeoutGuard<Double, Void>(timeout);

                    // ID takes precedence over address
                    var key = id == null ? address : id;

                    return (Map.Entry<String, Flux<Signal<Double, Void>>>) new ImmutablePair<>(key, guard.compute(flux));
                })
                .sequential();
    }

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
                        for (var spec : sensors) {
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
                    Optional.ofNullable(s.switches()).ifPresent(switches -> {
                        for (var spec : switches) {
                            switchConfigs.add(new MqttSwitchConfig(endpoint, spec));
                        }
                    });
                });
    }

    private final L resolveListener(MqttBrokerSpec address, MqttAdapter adapter) {
        return address2sensor.computeIfAbsent(address, k -> createSensorListener(adapter, address.rootTopic()));
    }

    protected abstract L createSensorListener(MqttAdapter adapter, String rootTopic);

    public Flux<Map.Entry<String, S>> getSwitches() {
        return getSwitches(endpoint2adapter, switchConfigs);
    }

    private Flux<Map.Entry<String, S>> getSwitches(Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter, Set<MqttSwitchConfig> source) {

        return Flux
                .fromIterable(source)
                .doOnNext(c -> logger.debug("switch: {}", c.switchConfig().address()))
                .doOnNext(c -> logger.debug("  broker: {}", c.mqttBrokerSpec().signature()))
                .doOnNext(c -> logger.debug("  endpoint: {}", endpoint2adapter.get(ConfigurationMapper.INSTANCE.parseEndpoint(c.mqttBrokerSpec())).address))
                .map(c -> {
                    var adapter = endpoint2adapter.get(ConfigurationMapper.INSTANCE.parseEndpoint(c.mqttBrokerSpec()));

                    var id = c.switchConfig().id();
                    var address = c.switchConfig.address();
                    var s = createSwitch(adapter, address, c.switchConfig.optimistic());

                    // ID takes precedence over address
                    var key = id == null ? address : id;

                    return new ImmutablePair<>(key, s);
                });
    }

    protected abstract S createSwitch(MqttAdapter adapter, String rootTopic, Boolean optimistic);

    public record MqttSensorConfig(
            MqttBrokerSpec mqttBrokerSpec,
            SensorConfig sensorConfig) {
    }

    public record MqttSwitchConfig(
            MqttBrokerSpec mqttBrokerSpec,
            SwitchConfig switchConfig) {
    }
}
