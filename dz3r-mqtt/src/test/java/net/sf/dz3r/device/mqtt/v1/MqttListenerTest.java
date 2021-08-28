package net.sf.dz3r.device.mqtt.v1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThatCode;

@EnabledIfEnvironmentVariable(
        named = "TEST_DZ_MQTT_REACTIVE",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker is available"
)
class MqttListenerTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void create() {
        assertThatCode(() -> {
            var ml = new MqttListener(new MqttEndpoint("localhost"));
        }).doesNotThrowAnyException();
    }

    @Test
    void subscribe() throws InterruptedException {

        assertThatCode(() -> {
            var ml = new MqttListener(new MqttEndpoint("localhost"));
            var subscription= ml
                    .getFlux("")
                    .doOnNext(v -> logger.info("message: {} {}", v.topic, v.message))
                    .subscribe();

            Thread.sleep(3_000);

            subscription.dispose();
        }).doesNotThrowAnyException();
    }
}
