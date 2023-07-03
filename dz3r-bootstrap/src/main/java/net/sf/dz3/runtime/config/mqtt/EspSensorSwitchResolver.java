package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.ConfigurationMapper;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.device.esphome.v1.ESPHomeListener;
import net.sf.dz3r.device.esphome.v1.ESPHomeSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.signal.Signal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;

public class EspSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig, ESPHomeListener, ESPHomeSwitch> {

    public EspSensorSwitchResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    @Override
    protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter, Set<MqttSensorConfig> source) {

        return Flux
                .fromIterable(source)
                .doOnNext(c -> logger.info("sensor: {}", c.sensorConfig().address()))
                .doOnNext(c -> logger.info("  broker: {}", c.mqttBrokerSpec().signature()))
                .doOnNext(c -> logger.info("  endpoint: {}", endpoint2adapter.get(ConfigurationMapper.INSTANCE.parseEndpoint(c.mqttBrokerSpec())).address))
                .map(c -> {
                    var adapter = endpoint2adapter.get(ConfigurationMapper.INSTANCE.parseEndpoint(c.mqttBrokerSpec()));
                    var listener = resolveListener(c.mqttBrokerSpec(), adapter);

                    return new ImmutablePair<>(c, listener);
                })

                // This is where things get hairy
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .map(kv -> {
                    var address = kv.getKey().sensorConfig().address();
                    var flux = kv.getValue().getFlux(address);

                    return new ImmutablePair<>(address, flux);
                })
                .sequential()
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .block();
    }

    @Override
    protected ESPHomeListener createSensorListener(MqttAdapter adapter, String rootTopic) {
        return new ESPHomeListener(adapter, rootTopic);
    }
}
