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

    private final String ZWAVE_SWITCH_TOPIC = "/esphome/7B13B1/switch/fan_panel_1";

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }
    @Test
    void setStateSync1010() {

        assertThatCode(() -> {

            var esphomeSwitch = new ESPHomeSwitchTest.SwitchWrapper(MQTT_BROKER, ZWAVE_SWITCH_TOPIC);

            // VT: NOTE: This switch doesn't control anything critical now, does it?

            var flux = Flux
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
            return super.getStateSync();
        }
    }
}
