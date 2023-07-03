package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.zwave.v1.ZWaveBinarySwitch;
import net.sf.dz3r.device.zwave.v1.ZWaveSensorListener;
import net.sf.dz3r.signal.SignalSource;

import java.util.Map;
import java.util.Set;

public class ZWaveSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig, SignalSource<String, Double, Void>, ZWaveBinarySwitch> {

    public ZWaveSensorSwitchResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    @Override
    protected SignalSource<String, Double, Void> createSensorListener(MqttAdapter adapter, String rootTopic) {
        return new ZWaveSensorListener(adapter.getAddress());
    }
}
