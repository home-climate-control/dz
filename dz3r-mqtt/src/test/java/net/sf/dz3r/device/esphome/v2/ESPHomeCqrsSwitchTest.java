package net.sf.dz3r.device.esphome.v2;

import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;

@EnabledIfEnvironmentVariable(
        named = "TEST_HCC_ESPHOME_SWITCH",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker and ESPHome switch device are available"
)
class ESPHomeCqrsSwitchTest {

    private final Logger logger = LogManager.getLogger();

    private final String MQTT_BROKER = "mqtt-esphome";

    private final String ESPHOME_SWITCH_TOPIC = "/esphome/81B190/switch/t-relay-3-r3";
    private final String ESPHOME_AVAILABILITY_TOPIC = "/esphome/81B190/status";

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    void setState1010() {

        assertThatCode(() -> {

            try (var adapter = new MqttAdapter(new MqttEndpoint(MQTT_BROKER))) {

                var esphomeSwitch = new ESPHomeCqrsSwitch(
                        "s",
                        Clock.systemUTC(),
                        null, null,
                        adapter,
                        ESPHOME_SWITCH_TOPIC,
                        ESPHOME_AVAILABILITY_TOPIC
                );

                // VT: NOTE: This switch doesn't control anything critical now, does it?

                var send = Flux
                        .just(true, false, true, false)
                        .delayElements(Duration.ofSeconds(1))
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(state -> {
                            logger.info("Switch state requested={}", state);
                            esphomeSwitch.setState(state);
                        });

                // This is likely to miss the last status update; good enough for now
                esphomeSwitch
                        .getFlux()
                        .subscribe(s -> logger.info("status: {}", s));

                send.blockLast();

                logger.info("DONE");
            }

        }).doesNotThrowAnyException();
    }
}
