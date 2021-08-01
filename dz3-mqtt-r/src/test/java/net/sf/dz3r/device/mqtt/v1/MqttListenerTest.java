package net.sf.dz3r.device.mqtt.v1;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class MqttListenerTest {

    @Test
    void create() {
        assertThatCode(() -> {
            var ml = new MqttListener(new MqttEndpoint("localhost"));
        }).doesNotThrowAnyException();
    }
}
