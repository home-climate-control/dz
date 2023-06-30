package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Set;

public class ZWaveSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig> {

    public ZWaveSensorSwitchResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    @Override
    protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(Set<MqttDeviceConfig> source) {
        logger.error("NOT IMPLEMENTED: {}#getSensorFluxes()", getClass().getName());
        return Map.of();
    }
}
