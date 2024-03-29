package net.sf.dz3r.runtime.config.mqtt;

import net.sf.dz3r.device.esphome.v1.ESPHomeListener;
import net.sf.dz3r.device.esphome.v2.ESPHomeCqrsSwitch;
import net.sf.dz3r.device.esphome.v2.ESPHomeFan;
import net.sf.dz3r.device.mqtt.MqttAdapter;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttEndpointSpec;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

public class ESPHomeDeviceResolver extends MqttDeviceResolver<MqttDeviceConfig, ESPHomeListener, ESPHomeCqrsSwitch, ESPHomeFan> {

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
    protected ESPHomeCqrsSwitch createSwitch(String id, Duration heartbeat, Duration pace, MqttAdapter adapter, String rootTopic, String availabilityTopic) {

        return new ESPHomeCqrsSwitch(
                id,
                Clock.systemUTC(),
                heartbeat,
                pace,
                adapter,
                rootTopic,
                availabilityTopic);
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
