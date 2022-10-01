package net.sf.dz3r.device.zwave.v1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@EnabledIfEnvironmentVariable(
        named = "TEST_DZ_ZWAVE_SWITCH",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker and Z-Wave switch device are available"
)
class BinarySwitchTest {

    private final Logger logger = LogManager.getLogger();

    private final String MQTT_BROKER = "mqtt-zwave";

    private final String ZWAVE_SWITCH_TOPIC = "zwave/TestLab/MP31ZP-0";

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }
    @Test
    void setStateSync1010() {

        assertThatCode(() -> {

            var zwaveSwitch = new BinarySwitch(MQTT_BROKER, ZWAVE_SWITCH_TOPIC);

            // VT: NOTE: This switch doesn't control anything critical now, does it?

            var flux = Flux
                    .just(true, false, true, false)
                    .delayElements(Duration.ofSeconds(1))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(state -> {
                        logger.info("Switch state: {}", state);
                        try {

                            zwaveSwitch.setStateSync(state);
                            assertThat(zwaveSwitch.getStateSync()).isEqualTo(state);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .blockLast();

            logger.info("DONE");

        }).doesNotThrowAnyException();
    }
}
