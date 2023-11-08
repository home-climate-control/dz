package net.sf.dz3r.device.mqtt.v2async;

import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;

@EnabledIfEnvironmentVariable(
        named = "TEST_HCC_MQTT_REACTIVE",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker is available"
)
class MqttListenerTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void create() {
        assertThatCode(() -> {
            new MqttListenerImpl(new MqttEndpoint("localhost"));
        }).doesNotThrowAnyException();
    }

    @Test
    void subscribe() {

        assertThatCode(() -> {
            new MqttListenerImpl(new MqttEndpoint("mqtt-esphome"))
                    .getFlux("", true)
                    .doOnNext(v -> logger.info("message: {}", v))
                    .take(Duration.ofSeconds(1))
                    .blockLast();
        }).doesNotThrowAnyException();
    }
}
