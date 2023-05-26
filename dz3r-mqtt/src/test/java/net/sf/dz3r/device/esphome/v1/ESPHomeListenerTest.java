package net.sf.dz3r.device.esphome.v1;

import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v1.MqttListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@EnabledIfEnvironmentVariable(
        named = "TEST_DZ_MQTT_REACTIVE",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker is available"
)
class ESPHomeListenerTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void constructor() {

        var mqttListener = new MqttListener(new MqttEndpoint("localhost"), null, null, false);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    new ESPHomeListener(mqttListener, "/won't get here");
                });
    }

    @Test
    void sensorFlux() throws InterruptedException {

        assertThatCode(() -> {
            var mqttListener = new MqttListener(new MqttEndpoint("localhost"), null, null, false);
            var esphomeListener = new ESPHomeListener(mqttListener, "/esphome");

            var subscription = esphomeListener.getFlux("dining-room")
                    .doOnNext(v -> logger.info("message: {}", v))
                    .take(10)
                    .subscribe();

            Thread.sleep(5_000);

            subscription.dispose();
        }).doesNotThrowAnyException();
    }
}
