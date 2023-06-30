package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.z2m.v1.Z2MJsonListener;
import net.sf.dz3r.device.z2m.v1.Z2MSwitch;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Set;

public class ZigbeeSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig, Z2MJsonListener, Z2MSwitch> {

    public ZigbeeSensorSwitchResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    @Override
    protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter, Set<MqttSensorConfig> source) {
        logger.error("NOT IMPLEMENTED: {}#getSensorFluxes()", getClass().getName());
//        source
//                .doOnNext(c -> logger.info("sensor: {}", c.sensorConfig().address()))
//                .collectList()
//                .block();

        return Map.of();
    }

    @Override
    protected Z2MJsonListener createSensorListener(MqttAdapter adapter, String rootTopic) {
        return null;
    }
}
