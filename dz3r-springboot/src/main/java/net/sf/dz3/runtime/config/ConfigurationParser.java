package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.protocol.mqtt.ESPHomeListenerConfig;
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

        var sensors = Flux
                .just(
                        new EspSensorFluxResolver(source.esphome()),
                        new OnewireSensorFluxResolver(source.onewire()),
                        new ZigbeeSensorFluxResolver(source.zigbee2mqtt()),
                        new ZWaveSensorFluxResolver(source.zwave2mqtt())
                )
                .map(SensorFluxResolver::getSensorFluxes)
                .blockLast();



        logger.error("ConfigurationParser::parse(): NOT IMPLEMENTED");
        return new HccParsedConfig();
    }

    private abstract class SensorFluxResolver<T> {

        protected final List<T> source;

        protected SensorFluxResolver(List<T> source) {
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

    private class EspSensorFluxResolver extends SensorFluxResolver<ESPHomeListenerConfig> {

        protected EspSensorFluxResolver(List<ESPHomeListenerConfig> source) {
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

    private class OnewireSensorFluxResolver extends SensorFluxResolver<OnewireBusConfig> {

        private OnewireSensorFluxResolver(List<OnewireBusConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<OnewireBusConfig> source) {
            return Map.of();
        }
    }
    private class ZigbeeSensorFluxResolver extends SensorFluxResolver<Z2MJsonListenerConfig> {

        private ZigbeeSensorFluxResolver(List<Z2MJsonListenerConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<Z2MJsonListenerConfig> source) {
            return Map.of();
        }
    }

    private class ZWaveSensorFluxResolver extends SensorFluxResolver<ZWaveListenerConfig> {

        private ZWaveSensorFluxResolver(List<ZWaveListenerConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<ZWaveListenerConfig> source) {
            return Map.of();
        }
    }
}
