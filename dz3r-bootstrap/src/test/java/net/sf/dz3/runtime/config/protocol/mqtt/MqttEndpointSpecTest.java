package net.sf.dz3.runtime.config.protocol.mqtt;

import net.sf.dz3.runtime.config.ConfigurationMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MqttEndpointSpecTest {

    @Test
    void equalsIdentical() {

        var ca = new MqttDeviceConfig(null,"localhost", null, null, null, "topicA", false, Set.of(), Set.of());
        var cb = new MqttDeviceConfig(null,"localhost", null, null, null, "topicA", false, Set.of(), Set.of());

        var ea = ConfigurationMapper.INSTANCE.parseEndpoint(ca);
        var eb = ConfigurationMapper.INSTANCE.parseEndpoint(cb);

        // This will fail with default equals()

        assertThat(ea).isEqualTo(eb);
    }
    @Test
    void equalsDifferentTopic() {

        var ca = new MqttDeviceConfig(null,"localhost", null, null, null, "topicA", false, Set.of(), Set.of());
        var cb = new MqttDeviceConfig(null,"localhost", null, null, null, "topicB", false, Set.of(), Set.of());

        var ea = ConfigurationMapper.INSTANCE.parseEndpoint(ca);
        var eb = ConfigurationMapper.INSTANCE.parseEndpoint(cb);

        // This will fail with default equals()

        assertThat(ea).isEqualTo(eb);
    }
}
