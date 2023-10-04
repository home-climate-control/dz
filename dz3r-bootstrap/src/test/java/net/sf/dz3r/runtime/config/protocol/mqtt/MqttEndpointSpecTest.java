package net.sf.dz3r.runtime.config.protocol.mqtt;

import net.sf.dz3r.runtime.config.ConfigurationMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MqttEndpointSpecTest {

    @Test
    void equalsIdentical() {

        var broker = new MqttBrokerConfig(null, "localhost", null, null, null, "topicA", false);

        var ca = new MqttDeviceConfig(broker, Set.of(), Set.of());
        var cb = new MqttDeviceConfig(broker, Set.of(), Set.of());

        var ea = ConfigurationMapper.INSTANCE.parseEndpoint(ca.broker());
        var eb = ConfigurationMapper.INSTANCE.parseEndpoint(cb.broker());

        // This will fail with default equals()

        assertThat(ea).isEqualTo(eb);
    }
    @Test
    void equalsDifferentTopic() {

        var brokerA = new MqttBrokerConfig(null, "localhost", null, null, null, "topicA", false);
        var brokerB = new MqttBrokerConfig(null, "localhost", null, null, null, "topicA", false);

        var ca = new MqttDeviceConfig(brokerA, Set.of(), Set.of());
        var cb = new MqttDeviceConfig(brokerB, Set.of(), Set.of());

        var ea = ConfigurationMapper.INSTANCE.parseEndpoint(ca.broker());
        var eb = ConfigurationMapper.INSTANCE.parseEndpoint(cb.broker());

        // This will fail with default equals()

        assertThat(ea).isEqualTo(eb);
    }
}
