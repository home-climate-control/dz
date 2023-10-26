package net.sf.dz3r.runtime.config.mqtt;

import net.sf.dz3r.device.actuator.VariableOutputDevice;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.z2m.v1.Z2MJsonListener;
import net.sf.dz3r.device.z2m.v1.Z2MSwitch;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttEndpointSpec;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

public class ZigbeeDeviceResolver extends MqttDeviceResolver<MqttDeviceConfig, Z2MJsonListener, Z2MSwitch, VariableOutputDevice> {

    public ZigbeeDeviceResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    /**
     * How long to wait before reporting a timeout.
     *
     * Behavior of Z2M devices is very different from
     * {@link net.sf.dz3r.device.esphome.v1.ESPHomeListener ESPHome based devices} - they may only report on a value
     * change, timeouts could be much higher, minutes, not seconds.
     *
     * @return 90 seconds. Not enough for decent control logic, but is a tolerable default.
     */
    @Override
    protected Duration getDefaultTimeout() {
        return Duration.ofSeconds(90);
    }

    @Override
    protected Z2MJsonListener createSensorListener(MqttAdapter adapter, String rootTopic) {
        return new Z2MJsonListener(adapter, rootTopic);
    }

    @Override
    protected Z2MSwitch createSwitch(MqttAdapter adapter, String rootTopic) {
        return new Z2MSwitch(
                adapter,
                rootTopic,
                false,
                null);
    }

    @Override
    protected VariableOutputDevice createFan(String id, Duration heartbeat, Duration pace, MqttAdapter adapter, String rootTopic, String availabilityTopic) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
