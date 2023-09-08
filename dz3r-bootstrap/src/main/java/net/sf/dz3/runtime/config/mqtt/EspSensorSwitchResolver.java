package net.sf.dz3.runtime.config.mqtt;

import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttEndpointSpec;
import net.sf.dz3r.device.esphome.v1.ESPHomeListener;
import net.sf.dz3r.device.esphome.v1.ESPHomeSwitch;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class EspSensorSwitchResolver extends MqttSensorSwitchResolver<MqttDeviceConfig, ESPHomeListener, ESPHomeSwitch> {

    public EspSensorSwitchResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    @Override
    protected ESPHomeListener createSensorListener(MqttAdapter adapter, String rootTopic) {
        return new ESPHomeListener(adapter, rootTopic);
    }

    @Override
    protected ESPHomeSwitch createSwitch(MqttAdapter adapter, String rootTopic, Boolean optimistic) {

        // Optimistic defaults to true for this switch only
        // https://github.com/home-climate-control/dz/issues/280

        return new ESPHomeSwitch(
                adapter,
                rootTopic,
                Optional.ofNullable(optimistic).orElse(true),
                null);
    }
}
