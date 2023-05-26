package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.protocol.mqtt.ESPHomeListenerConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttGateway;
import net.sf.dz3.runtime.config.protocol.mqtt.Z2MJsonListenerConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.ZWaveListenerConfig;
import net.sf.dz3.runtime.config.protocol.onewire.OnewireBusConfig;
import net.sf.dz3r.device.esphome.v1.ESPHomeListener;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.signal.Signal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
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

    /**
     * Mapping from (host, port) to {@link ESPHomeListener} instance for that endpoint.
     */
    private Map<String, ESPHomeListener> esphomeMapping = new LinkedHashMap<>();

    public HccParsedConfig parse(HccRawConfig source) {

        // VT: FIXME: Leaving 1-Wire in the dust for now

        // Step 1: collect all MQTT endpoints and get their configurations

        var endpoints = Flux
                .just(
                        new EspSensorSwitchResolver(source.esphome()),
                        new ZigbeeSensorSwitchResolver(source.zigbee2mqtt()),
                        new ZWaveSensorSwitchResolver(source.zwave2mqtt())
                )
                .flatMap(MqttSensorSwitchResolver::getEndpoints)
                .doOnNext(endpoint -> logger.info("endpoint found: {}", endpoint.signature()))
                .collectList()
                .block();

        logger.info("all endpoints: {}", endpoints);

        // Step 2: for all the endpoints, materialize their brokers

        // Step 2: for all the brokers, collect all their sensors and switches, in parallel

        var sensors = Flux
                .just(
                        new EspSensorSwitchResolver(source.esphome()),
                        new OnewireSensorSwitchResolver(source.onewire()),
                        new ZigbeeSensorSwitchResolver(source.zigbee2mqtt()),
                        new ZWaveSensorSwitchResolver(source.zwave2mqtt())
                )
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

            Flux<MqttEndpointSpec> result = Flux.create(sink -> {

                var sequence = Flux
                        .fromIterable(source)

                        .map(item -> new ImmutablePair<>(item.signature(), item))
                        .doOnNext(kv -> {

                            var key = kv.getKey();
                            if (!endpoints.contains(key)) {
                                sink.next(parse(kv.getValue()));
                                endpoints.add(key);
                            }
                        })
                        .subscribe();

                sink.complete();

                sequence.dispose();
            });

            return result;
        }

        private MqttEndpointSpec parse(MqttGateway source) {
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
    }

    private class EspSensorSwitchResolver extends MqttSensorSwitchResolver<ESPHomeListenerConfig> {

        protected EspSensorSwitchResolver(List<ESPHomeListenerConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<ESPHomeListenerConfig> source) {

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

                        Flux<Pair<ESPHomeListener, String>> result =  Flux.create(sink -> {
                            for (var address : triple.getMiddle()) {
                                sink.next(new ImmutablePair<>(listener, address.address()));
                            }
                            sink.complete();
                        });

                        return result;
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

    private class ZigbeeSensorSwitchResolver extends MqttSensorSwitchResolver<Z2MJsonListenerConfig> {

        private ZigbeeSensorSwitchResolver(List<Z2MJsonListenerConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<Z2MJsonListenerConfig> source) {
            return Map.of();
        }
    }

    private class ZWaveSensorSwitchResolver extends MqttSensorSwitchResolver<ZWaveListenerConfig> {

        private ZWaveSensorSwitchResolver(List<ZWaveListenerConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<ZWaveListenerConfig> source) {
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
}
