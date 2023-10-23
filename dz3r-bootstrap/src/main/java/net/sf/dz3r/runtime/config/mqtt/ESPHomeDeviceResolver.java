package net.sf.dz3r.runtime.config.mqtt;

import net.sf.dz3r.device.esphome.v1.ESPHomeListener;
import net.sf.dz3r.device.esphome.v1.ESPHomeSwitch;
import net.sf.dz3r.device.esphome.v2.ESPHomeFan;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttEndpointSpec;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ESPHomeDeviceResolver extends MqttDeviceResolver<MqttDeviceConfig, ESPHomeListener, ESPHomeSwitch, ESPHomeFan> {

    public ESPHomeDeviceResolver(Set<MqttDeviceConfig> source, Map<MqttEndpointSpec, MqttAdapter> endpoint2adapter) {
        super(source, endpoint2adapter);
    }

    @Override
    protected Duration getDefaultTimeout() {
        // Enough for decent control quality
        return Duration.ofSeconds(30);
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

    @Override
    protected ESPHomeFan createFan(String id, Duration heartbeat, Duration pace, MqttAdapter adapter, String rootTopic, String availabilityTopic) {
        return new ESPHomeFan(
                id,
                Clock.systemUTC(),
                heartbeat,
                pace,
                adapter,
                rootTopic,
                availabilityTopic
        );
    }
}
