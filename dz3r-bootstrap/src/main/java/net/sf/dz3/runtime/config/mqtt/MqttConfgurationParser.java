package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.SensorSwitchResolver;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;

public class MqttConfgurationParser {

    private final Logger logger = LogManager.getLogger();
    public void parse(Set<MqttDeviceConfig> esphome, Set<MqttDeviceConfig> zigbee2mqtt, Set<MqttDeviceConfig> zwave2mqtt) {

        var endpoint2adapter = Collections.synchronizedMap(new LinkedHashMap<MqttEndpointSpec, MqttAdapter>());

        var mqttConfigs = Flux
                .just(
                        new EspSensorSwitchResolver(esphome, endpoint2adapter),
                        new ZigbeeSensorSwitchResolver(zigbee2mqtt, endpoint2adapter),
                        new ZWaveSensorSwitchResolver(zwave2mqtt, endpoint2adapter)
                )
                // We'll have to walk through this more than once
                .share();

        // Step 1: collect all MQTT endpoints and get their configurations

        var endpoints = mqttConfigs
                .flatMap(MqttSensorSwitchResolver::getEndpoints)
                .doOnNext(endpoint -> logger.info("endpoint found: {}", endpoint.signature()));

        // Step 2: for all the endpoints, materialize their brokers - they will be started later

        endpoints
                .map(endpoint -> new EndpointAdapterPair(
                        endpoint,
                        new MqttAdapter(
                                new MqttEndpoint(endpoint.host(), Optional.ofNullable(endpoint.port()).orElse(MqttEndpoint.DEFAULT_PORT)),
                                endpoint.username(),
                                endpoint.password(),
                                endpoint.autoReconnect())))
                .doOnNext(kv -> {
                    // Need to inject the resolved pairs into resolvers so they don't have to do it again
                    endpoint2adapter.put(kv.endpoint, kv.adapter);
                })

                // ... and we'll just have to wait until this is done.
                .blockLast();

        // Step 2: for all the brokers, collect all their sensors and switches

        var broker2sensor = mqttConfigs
                .flatMap(MqttSensorSwitchResolver::getSensorConfigs)
                .doOnNext(kv -> logger.warn("{} : {}", kv.getKey().signature(), kv.getValue()))
                .collectMap(kv -> kv.getKey(), kv -> kv.getValue())
                .block();

        var broker2switch = mqttConfigs
                .flatMap(MqttSensorSwitchResolver::getSwitchConfigs)
                .doOnNext(kv -> logger.warn("{} : {}", kv.getKey().signature(), kv.getValue()))
                .collectMap(kv -> kv.getKey(), kv -> kv.getValue())
                .block();

        // Step 3: now combine all of those into a single stream of sensors, and another of switches

        var sensors = mqttConfigs
                .map(SensorSwitchResolver::getSensorFluxes)
                .collectList()
                .block();
    }

    private record EndpointAdapterPair (
            MqttEndpointSpec endpoint,
            MqttAdapter adapter
    ) {

    }
}
