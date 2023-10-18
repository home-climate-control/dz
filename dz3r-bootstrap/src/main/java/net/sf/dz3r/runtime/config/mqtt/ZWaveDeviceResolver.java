package net.sf.dz3r.runtime.config.mqtt;

import net.sf.dz3r.device.actuator.VariableOutputDevice;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.zwave.v1.ZWaveBinarySwitch;
import net.sf.dz3r.device.zwave.v1.ZWaveSensorListener;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.signal.SignalSource;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ZWaveDeviceResolver extends MqttDeviceResolver<MqttDeviceConfig, SignalSource<String, Double, Void>, ZWaveBinarySwitch, VariableOutputDevice> {

    public ZWaveDeviceResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    /**
     * How long to wait before reporting a timeout.
     *
     * Behavior of Z-Wave devices is very different from
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
    protected SignalSource<String, Double, Void> createSensorListener(MqttAdapter adapter, String rootTopic) {
        return new ZWaveSensorListener(adapter.getAddress());
    }

    @Override
    protected ZWaveBinarySwitch createSwitch(MqttAdapter adapter, String rootTopic, Boolean optimistic) {
        return new ZWaveBinarySwitch(
                adapter,
                rootTopic,
                Optional.ofNullable(optimistic).orElse(false),
                null);
    }

    @Override
    protected VariableOutputDevice createFan(String id, MqttAdapter adapter, String rootTopic, String availabilityTopic) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
