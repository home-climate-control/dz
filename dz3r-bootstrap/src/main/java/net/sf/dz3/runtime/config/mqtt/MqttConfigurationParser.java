package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parser for all MQTT based device configurations (that'll be ESPHome, Zigbee, Z-Wave at the moment).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class MqttConfigurationParser {

    private final Logger logger = LogManager.getLogger();

    /**
     * Take configurations; produce device fluxes.
     *
     * @param esphome Set of ESPHome device configurations.
     * @param zigbee2mqtt Set of Zigbee2MQTT device configurations.
     * @param zwave2mqtt Set of ZWave2MQTT device configurations.
     */
    public void parse(Set<MqttDeviceConfig> esphome, Set<MqttDeviceConfig> zigbee2mqtt, Set<MqttDeviceConfig> zwave2mqtt) {

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
                    .flatMap(MqttSensorSwitchResolver::getEndpoints)

                    // Previous step may have yielded dupes if different sets have the same endpoints
                    .collect(Collectors.toSet())
                    .block();

            // Step 2: for all the endpoints, materialize their adapters - brokers will be created later
            // Need to inject the resolved pairs into resolvers so they don't have to do it again

            Flux.fromIterable(Optional.ofNullable(endpoints).orElseThrow(() -> new IllegalStateException("Impossible")))
                    .subscribe(endpoint -> endpoint2adapter.put(endpoint,
                            new MqttAdapter(
                                    new MqttEndpoint(endpoint.host(), Optional.ofNullable(endpoint.port()).orElse(MqttEndpoint.DEFAULT_PORT)),
                                    endpoint.username(),
                                    endpoint.password(),
                                    endpoint.autoReconnect())));

            // Step 3: for all the brokers, collect all their sensors and switches

            mqttConfigs
                    .doOnNext(MqttSensorSwitchResolver::getSensorConfigs)
                    .blockLast();

            mqttConfigs
                    .doOnNext(MqttSensorSwitchResolver::getSwitchConfigs)
                    .blockLast();

            // Step 4: now combine all of those into a single stream of sensors, and another of switches.
            // Each of resolvers knows exact listener or adapter configuration for a specific device type.

            // VT: FIXME: These will be returned as a flux when the whole config parser is stitched together.

            var sensors = mqttConfigs
                    .map(MqttSensorSwitchResolver::getSensorFluxes)
                    .doOnNext(e -> {
                        logger.info("sensorFlux: {}", e.keySet());
                        Flux
                                .fromIterable(e.values())
                                .doOnNext(f -> f.subscribeOn(Schedulers.boundedElastic()).subscribe())
                                .subscribe();
                    })
                    .blockLast();
//                .collectList()
//                .block();

            var switches = mqttConfigs
                    .flatMap(MqttSensorSwitchResolver::getSwitches)
                    .doOnNext(s -> {
                        logger.info("switch: {}", s.getAddress());
                    })
                    .blockLast();
        } finally {
            m.close();
        }
    }
}
