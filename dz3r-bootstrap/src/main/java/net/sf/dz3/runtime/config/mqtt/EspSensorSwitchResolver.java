package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttGateway;
import net.sf.dz3r.device.esphome.v1.ESPHomeListener;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.signal.Signal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public class EspSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig> {

    public EspSensorSwitchResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    @Override
    protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(Set<MqttDeviceConfig> source) {

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
                    for (var spec : kv.getValue()) {
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
