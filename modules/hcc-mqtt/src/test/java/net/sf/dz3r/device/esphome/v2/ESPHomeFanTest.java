package net.sf.dz3r.device.esphome.v2;

import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v2async.MqttAdapterImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;

import static net.sf.dz3r.device.actuator.VariableOutputDevice.Command;
import static org.assertj.core.api.Assertions.assertThatCode;

@EnabledIfEnvironmentVariable(
        named = "TEST_HCC_ESP_FAN",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker and ESP Fan device are available"
)
class ESPHomeFanTest {

    private final Logger logger = LogManager.getLogger();

    private final String MQTT_BROKER = "mqtt-esphome";
    private final String FAN_TOPIC = "/esphome/550212/fan/a6-0";
    private final String AVAILABILITY_TOPIC = "/esphome/550212/status";

    @Test
    void sendCycle() throws Exception {

        assertThatCode(() -> {

            var adapter = new MqttAdapterImpl(new MqttEndpoint(MQTT_BROKER));
            var fan = new ESPHomeFan(
                    "a6",
                    Clock.systemUTC(),
                    null,
                    null,
                    adapter,
                    FAN_TOPIC,
                    AVAILABILITY_TOPIC
            );

            var status = fan
                    .getFlux()
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(state -> logger.info("state/rcvd: {}", state))
                    .subscribe();

            Flux
                    .just(0d, 0.25d, 0.5d, 0.75d, 1d)
                    .delayElements(Duration.ofSeconds(1))
                    .map(level -> fan.setState(new Command(true, level)))
                    .doOnNext(state -> logger.info("state/sent: {}", state))
                    .blockLast();

            Thread.sleep(1000); // NOSONAR Too much hassle

            fan.close();

            logger.info("final state: {}", fan.getState());
            status.dispose();

        }).doesNotThrowAnyException();
    }
}
