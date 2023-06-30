package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.SensorSwitchResolver;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

public class MqttConfgurationParser {

    private final Logger logger = LogManager.getLogger();
    public void parse(Flux<MqttSensorSwitchResolver<MqttDeviceConfig>> source) {

        // We'll have to walk through this more than once
        var mqttConfigs = source.share();

        // Step 1: collect all MQTT endpoints and get their configurations

        var endpoints = mqttConfigs
                .flatMap(MqttSensorSwitchResolver::getEndpoints)
                .doOnNext(endpoint -> logger.info("endpoint found: {}", endpoint.signature()));

        // Step 2: for all the endpoints, materialize their brokers in parallel

        var endpoint2adapter = endpoints
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .map(endpoint -> new ImmutablePair(
                        endpoint,
                        new MqttAdapter(
                                new MqttEndpoint(endpoint.host(), Optional.ofNullable(endpoint.port()).orElse(MqttEndpoint.DEFAULT_PORT)),
                                endpoint.username(),
                                endpoint.password(),
                                endpoint.autoReconnect())))

                // Now that they've all been created, let's leave this hanging for consumption below.
                .sequential()
                .collectMap(kv -> kv.getKey(), kv -> kv.getValue())
                .block();

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
}
