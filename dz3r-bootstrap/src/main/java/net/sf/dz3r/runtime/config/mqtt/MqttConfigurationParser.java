package net.sf.dz3r.runtime.config.mqtt;

import net.sf.dz3r.device.mqtt.v1.AbstractMqttSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.runtime.config.ConfigurationMapper;
import net.sf.dz3r.runtime.config.connector.HomeAssistantConfig;
import net.sf.dz3r.runtime.config.connector.HomeAssistantConfigParser;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.signal.Signal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parser for all MQTT based device configurations (that'll be ESPHome, Zigbee, Z-Wave at the moment).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttConfigurationParser extends ConfigurationContextAware {

    public MqttConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    /**
     * Take configurations; produce device fluxes.
     *
     * @param esphome Set of ESPHome device configurations.
     * @param zigbee2mqtt Set of Zigbee2MQTT device configurations.
     * @param zwave2mqtt Set of ZWave2MQTT device configurations.
     * @param ha Set of Home Assistant configurations.
     *
     * @return a {@link Mono} that gets completed when every sensor and switch configuration is resolved. Payload is irrelevant
     * and is to be used for informational purposes and flow control only, {@link #context} is where the useful information
     * is gathered.
     */
    public Mono<Tuple2<
            List<Map.Entry<String, Flux<Signal<Double, Void>>>>,
            List<Map.Entry<String, AbstractMqttSwitch>>>> parse(
            Set<MqttDeviceConfig> esphome,
            Set<MqttDeviceConfig> zigbee2mqtt,
            Set<MqttDeviceConfig> zwave2mqtt,
            Set<HomeAssistantConfig> ha) {

        Marker m = new Marker(getClass().getSimpleName() + "#parse");
        try {

            var endpoint2adapter = new LinkedHashMap<MqttEndpointSpec, MqttAdapter>();

            var mqttConfigs = Flux
                    .just(
                            new EspSensorSwitchResolver(esphome, endpoint2adapter),
                            new ZigbeeSensorSwitchResolver(zigbee2mqtt, endpoint2adapter),
                            new ZWaveSensorSwitchResolver(zwave2mqtt, endpoint2adapter)
                    );

            // Step 1: collect all MQTT endpoints and get their configurations

            var endpoints = mqttConfigs
                    .flatMap(MqttSensorSwitchResolver::getEndpoints);

            var haEndpoints = Flux
                    .fromIterable(ha)
                    .map(HomeAssistantConfigParser::parse)
                    .map(ConfigurationMapper.INSTANCE::parseEndpoint);

            var allEndpoints = Flux.merge(endpoints, haEndpoints)

                    // Previous step may have yielded dupes if different sets have the same endpoints
                    .collect(Collectors.toSet())
                    .block();

            // Step 2: for all the endpoints, materialize their adapters - brokers will be created later
            // Need to inject the resolved pairs into resolvers, so they don't have to do it again

            Flux.fromIterable(Optional.ofNullable(allEndpoints).orElseThrow(() -> new IllegalStateException("Impossible")))
                    .subscribe(endpoint -> {

                        var adapter = new MqttAdapter(
                                new MqttEndpoint(endpoint.host(), Optional.ofNullable(endpoint.port()).orElse(MqttEndpoint.DEFAULT_PORT)),
                                endpoint.username(),
                                endpoint.password(),
                                endpoint.autoReconnect());

                        context.mqtt.register(endpoint.signature(), adapter);
                        endpoint2adapter.put(endpoint, adapter);
                    });

            // Step 3: for all the brokers, collect all their sensors and switches

            mqttConfigs
                    .doOnNext(MqttSensorSwitchResolver::getSensorConfigs)
                    .blockLast();

            mqttConfigs
                    .doOnNext(MqttSensorSwitchResolver::getSwitchConfigs)
                    .blockLast();

            // Step 4: now combine all of those into a single stream of sensors, and another of switches.
            // Each of resolvers knows exact listener or adapter configuration for a specific device type.

            var m2 = new Marker(getClass().getSimpleName() + "#gate");
            var sensors = mqttConfigs
                    .publishOn(Schedulers.boundedElastic())
                    .flatMap(MqttSensorSwitchResolver::getSensorFluxes)
                    .doOnNext(kv -> context.sensors.register(kv.getKey(), kv.getValue()))
                    .collectList();

            var switches = mqttConfigs
                    .publishOn(Schedulers.boundedElastic())
                    .flatMap(MqttSensorSwitchResolver::getSwitches)
                    .doOnNext(kv -> context.switches.register(kv.getKey(), kv.getValue()))
                    .map(kv -> ((Map.Entry<String, AbstractMqttSwitch>) new ImmutablePair<>(kv.getKey(), kv.getValue())))
                    .collectList();

            logger.debug("waiting at the gate");

            return Mono.create(sink -> {
                var result = Mono
                        .zip(sensors, switches)
                        .block();

                logger.debug("passed the gate");
                m2.close();

                sink.success(result);
            });

        } finally {
            m.close();
        }
    }
}
