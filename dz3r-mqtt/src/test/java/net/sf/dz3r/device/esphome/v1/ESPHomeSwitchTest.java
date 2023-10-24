package net.sf.dz3r.device.esphome.v1;

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
        named = "TEST_DZ_ESPHOME_SWITCH",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker and ESPHome switch device are available"
)
class ESPHomeSwitchTest {

    private final Logger logger = LogManager.getLogger();

    private final String MQTT_BROKER = "mqtt-esphome";

    private final String ESPHOME_SWITCH_TOPIC = "/esphome/81B190/switch/t-relay-3-r3";

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }
    @Test
    void setStateSync1010() {

        assertThatCode(() -> {

            var esphomeSwitch = new ESPHomeSwitchTest.SwitchWrapper(MQTT_BROKER, ESPHOME_SWITCH_TOPIC);

            // VT: NOTE: This switch doesn't control anything critical now, does it?

            Flux
                    .just(true, false, true, false)
                    .delayElements(Duration.ofSeconds(1))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(state -> {
                        logger.info("Switch state: {}", state);
                        try {

                            esphomeSwitch.setStateSync(state);
                            assertThat(esphomeSwitch.getStateSync()).isEqualTo(state);

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

        var esphomeSwitch = new ESPHomeSwitch(MQTT_BROKER, ESPHOME_SWITCH_TOPIC);

        assertThatCode(() -> {
            Flux
                    .just(true, false, true, false)
                    .delayElements(Duration.ofSeconds(1))
                    .flatMap(esphomeSwitch::setState)
                    .doOnNext(state -> logger.info("state: {}", state))
                    .blockLast();

        }).doesNotThrowAnyException();
    }

    private static class SwitchWrapper extends ESPHomeSwitch {

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
