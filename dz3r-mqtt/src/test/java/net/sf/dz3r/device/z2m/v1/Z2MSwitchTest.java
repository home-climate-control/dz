package net.sf.dz3r.device.z2m.v1;


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
        named = "TEST_DZ_Z2M_SWITCH",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker and Zigbee switch device are available"
)
class Z2MSwitchTest {

    private final Logger logger = LogManager.getLogger();

    private final String MQTT_BROKER = "mqtt-zigbee";

    private final String ZIGBEE_SWITCH_TOPIC = "zigbee2mqtt-dev/sengled-01";

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }
    @Test
    void setStateSync1010() {

        assertThatCode(() -> {

            var zigbeeSwitch = new SwitchWrapper(MQTT_BROKER, ZIGBEE_SWITCH_TOPIC);

            // VT: NOTE: This switch doesn't control anything critical now, does it?

            Flux
                    .just(true, false, true, false)
                    .delayElements(Duration.ofSeconds(1))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(state -> {
                        logger.info("Switch state: {}", state);
                        try {

                            zigbeeSwitch.setStateSync(state);
                            assertThat(zigbeeSwitch.getStateSync()).isEqualTo(state);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .blockLast();

            logger.info("DONE");

        }).doesNotThrowAnyException();
    }

    @Test
    void testFlux1010() {

        var zigbeeSwitch = new Z2MSwitch(MQTT_BROKER, ZIGBEE_SWITCH_TOPIC);

        assertThatCode(() -> {
            Flux
                    .just(true, false, true, false)

                    // Zigbee works MUCH faster than Z-Wave
                    .delayElements(Duration.ofMillis(500))

                    .flatMap(zigbeeSwitch::setState)
                    .doOnNext(state -> logger.info("state: {}", state))
                    .blockLast();

        }).doesNotThrowAnyException();
    }
    private static class SwitchWrapper extends Z2MSwitch {

        protected SwitchWrapper(String host, String deviceRootTopic) {
            super(host, deviceRootTopic);
        }

        @Override
        public void setStateSync(boolean state) throws IOException {
            super.setStateSync(state);
        }
        @Override
        public boolean getStateSync() throws IOException {
            return getState().block();
        }
    }
}
