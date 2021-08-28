package net.sf.dz3r.device.esphome.v1;

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
class ESPHomeListenerTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void sensorFlux() throws InterruptedException {

        assertThatCode(() -> {
            var l = new ESPHomeListener("localhost", "/esphome");

            var subscription = l.getFlux("dining-room")
                    .doOnNext(v -> logger.info("message: {}", v))
                    .take(10)
                    .subscribe();

            Thread.sleep(5_000);

            subscription.dispose();
        }).doesNotThrowAnyException();
    }
}
