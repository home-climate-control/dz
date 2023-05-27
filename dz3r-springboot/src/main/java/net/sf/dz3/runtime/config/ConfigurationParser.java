package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.hardware.SensorConfig;
import net.sf.dz3.runtime.config.hardware.SwitchConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttBrokerSpec;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttGateway;
import net.sf.dz3.runtime.config.protocol.onewire.OnewireBusConfig;
import net.sf.dz3r.device.esphome.v1.ESPHomeListener;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.signal.Signal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Parses {@link HccRawConfig} into {@link HccParsedConfig}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ConfigurationParser {
    private final Logger logger = LogManager.getLogger();

    public HccParsedConfig parse(HccRawConfig source) {

        // VT: FIXME: Leaving 1-Wire in the dust for now

        // Step 1: Collect all MQTT configs into one place

        var mqttConfigs = Flux
                .just(
                        new EspSensorSwitchResolver(source.esphome()),
                        new ZigbeeSensorSwitchResolver(source.zigbee2mqtt()),
                        new ZWaveSensorSwitchResolver(source.zwave2mqtt())
                )
                .share();

        // Step 2: collect all MQTT endpoints and get their configurations

        var endpoint2adapter = mqttConfigs
                .flatMap(MqttSensorSwitchResolver::getEndpoints)
                .doOnNext(endpoint -> logger.info("endpoint found: {}", endpoint.signature()))

        // Step 3: for all the endpoints, materialize their brokers in parallel

                .parallel()
                .runOn(Schedulers.boundedElastic())
                .map(e -> new ImmutablePair(
                        e,
                        new MqttAdapter(
                                new MqttEndpoint(e.host(), Optional.ofNullable(e.port()).orElse(MqttEndpoint.DEFAULT_PORT)),
                                e.username(),
                                e.password(),
                                e.autoReconnect())))

                // Now that they've all been created, let's leave this hanging for consumption below
                .sequential()
                .blockLast();

        // Step 4: for all the brokers, collect all their sensors and switches

        var broker2sensor = mqttConfigs
                .flatMap(MqttSensorSwitchResolver::getSensorConfigs)
                .doOnNext(kv -> logger.warn("{} {} : {}", kv.getKey().signature(), kv.getKey().rootTopic(), kv.getValue()))
                .blockLast();

        var broker2switch = mqttConfigs
                .flatMap(MqttSensorSwitchResolver::getSwitchConfigs)
                .doOnNext(kv -> logger.warn("{} {} : {}", kv.getKey().signature(), kv.getKey().rootTopic(), kv.getValue()))
                .blockLast();

        // Step 5: now combine all of those into a single stream of sensors, and another of switches

        var sensors = mqttConfigs
                .map(SensorSwitchResolver::getSensorFluxes)
                .blockLast();



        logger.error("ConfigurationParser::parse(): NOT IMPLEMENTED");
        return new HccParsedConfig();
    }

    /**
     * Resolves switches and sensors from configuration elements.
     *
     * @param <T> Configuration element type.
     */
    private abstract class SensorSwitchResolver<T> {

        protected final List<T> source;

        protected SensorSwitchResolver(List<T> source) {
            this.source = source;
        }

        /**
         * Parse the configuration into the mapping from the flux ID to the flux.
         *
         * @return Map of (flux ID, flux) for all the given sources.
         */
        protected abstract Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<T> source);

        public final Map<String, Flux<Signal<Double, Void>>> getSensorFluxes() {
            logger.error("NOT IMPLEMENTED: {}#getSensorFluxes()", getClass().getName());
            return getSensorFluxes(source);
        }
    }

    private abstract class MqttSensorSwitchResolver<T extends MqttGateway> extends SensorSwitchResolver<T> {

        protected MqttSensorSwitchResolver(List<T> source) {
            super(source);
        }

        public final Flux<MqttEndpointSpec> getEndpoints() {

            var endpoints = new LinkedHashSet<String>();

            return Flux.<MqttEndpointSpec>create(sink -> {

                var sequence = Flux
                        .fromIterable(source)

                        .map(item -> new ImmutablePair<>(item.signature(), item))
                        .doOnNext(kv -> {

                            var key = kv.getKey();
                            if (!endpoints.contains(key)) {
                                sink.next(parseEndpoint(kv.getValue()));
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

            return Flux.<Pair<MqttBrokerSpec, SensorConfig>>create(sink -> {
                var sequence = Flux
                        .fromIterable(source)
                        .doOnNext(s -> {
                            var endpoint = parseBroker(s);
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

            return Flux.<Pair<MqttBrokerSpec, SwitchConfig>>create(sink -> {
                var sequence = Flux
                        .fromIterable(source)
                        .doOnNext(s -> {
                            var endpoint = parseBroker(s);
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

    private class EspSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig> {

        protected EspSensorSwitchResolver(List<MqttDeviceConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<MqttDeviceConfig> source) {

            var collector = new TreeMap<String, Set<MqttGateway>>();

            var endpoints = Flux
                    .fromIterable(source)

                    // Collect items from possibly different profile configurations into a single packet
                    .map(item -> new ImmutablePair<>(item.signature(), item))
                    .doOnNext(item -> collector
                            .computeIfAbsent(item.getKey(), k -> new LinkedHashSet<>())
                            .add(item.getValue()))
                    .subscribe();

            Flux
                    .fromIterable(collector.entrySet())
                    .doOnNext(kv -> {
                        logger.info("endpoint: {}", kv.getKey());
                        for (var spec: kv.getValue()) {
                            logger.info("  {}", spec);
                        }
                    })
                    .subscribe();

            endpoints.dispose();

            Flux
                    .fromIterable(collector.entrySet())

                    // We don't need the keys anymore, they played their role
                    .flatMap(kv -> Flux.fromIterable(kv.getValue()))

                    // From now on, entries point to distinctly different MQTT sources
                    // Let's start them in parallel to speed up execution

                    .parallel()
                    .runOn(Schedulers.boundedElastic())

                    .map(spec -> new ImmutableTriple<>(
                            new ESPHomeListener(
                                    spec.host(),
                                    Optional.ofNullable(spec.port()).orElse(MqttEndpoint.DEFAULT_PORT),
                                    spec.username(),
                                    spec.password(),
                                    spec.autoReconnect(),
                                    spec.rootTopic()),
                            spec.sensors(),
                            spec.switches()))
                    .flatMap(triple -> {
                        // VT: FIXME: Ignoring switches for now
                        var listener = triple.getLeft();

                        return Flux.<Pair<ESPHomeListener, String>>create(sink -> {
                            for (var address : triple.getMiddle()) {
                                sink.next(new ImmutablePair<>(listener, address.address()));
                            }
                            sink.complete();
                        });
                    })

                    // Reshuffle, the cadence is different here
                    .sequential()
                    .parallel()
                    .runOn(Schedulers.boundedElastic())

                    .map(kv -> kv.getKey().getFlux(kv.getValue()))
                    .map(flux -> flux.subscribe(s -> logger.info("signal: {}", s)))
                    .sequential()

                    .blockLast();

            return Map.of();
        }
    }

    private class ZigbeeSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig> {

        private ZigbeeSensorSwitchResolver(List<MqttDeviceConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<MqttDeviceConfig> source) {
            return Map.of();
        }
    }

    private class ZWaveSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig> {

        private ZWaveSensorSwitchResolver(List<MqttDeviceConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<MqttDeviceConfig> source) {
            return Map.of();
        }
    }

    private class OnewireSensorSwitchResolver extends SensorSwitchResolver<OnewireBusConfig> {

        private OnewireSensorSwitchResolver(List<OnewireBusConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<OnewireBusConfig> source) {
            return Map.of();
        }
    }

    private MqttEndpointSpec parseEndpoint(MqttGateway source) {
        return new MqttEndpointSpec() {
            @Override
            public String host() {
                return source.host();
            }

            @Override
            public Integer port() {
                return source.port();
            }

            @Override
            public boolean autoReconnect() {
                return source.autoReconnect();
            }

            @Override
            public String username() {
                return source.username();
            }

            @Override
            public String password() {
                return source.password();
            }
        };
    }

    private MqttBrokerSpec parseBroker(MqttGateway source) {
        return new MqttBrokerSpec() {
            @Override
            public String host() {
                return source.host();
            }

            @Override
            public Integer port() {
                return source.port();
            }

            @Override
            public boolean autoReconnect() {
                return source.autoReconnect();
            }

            @Override
            public String username() {
                return source.username();
            }

            @Override
            public String password() {
                return source.password();
            }

            @Override
            public String rootTopic() {
                return source.rootTopic();
            }
        };
    }
}
