package net.sf.dz3r.runtime.config.mqtt;

import net.sf.dz3r.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.z2m.v1.Z2MJsonListener;
import net.sf.dz3r.device.z2m.v1.Z2MSwitch;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ZigbeeSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig, Z2MJsonListener, Z2MSwitch> {

    public ZigbeeSensorSwitchResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    @Override
    protected Z2MJsonListener createSensorListener(MqttAdapter adapter, String rootTopic) {
        return new Z2MJsonListener(adapter, rootTopic);
    }

    @Override
    protected Z2MSwitch createSwitch(MqttAdapter adapter, String rootTopic, Boolean optimistic) {
        return new Z2MSwitch(
                adapter,
                rootTopic,
                Optional.ofNullable(optimistic).orElse(false),
                null);
    }
}
