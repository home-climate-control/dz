package net.sf.dz3r.device.actuator;

import net.sf.dz3r.device.esphome.v1.ESPHomeSwitch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Test MQTT switch race conditions when controlled by a heat pump.
 *
 * VT: NOTE: Careful, this works on real hardware, disconnect before testing.
 */
@EnabledIfEnvironmentVariable(
        named = "TEST_DZ_ESPHOME_HEATPUMP",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker and ESPHome switch device are available"
)class ESPHomeHeatPumpTest {

    private final Logger logger = LogManager.getLogger();

    private final String MQTT_BROKER = "mqtt-esphome";

    private final String ESPHOME_SWITCH_TOPIC_ROOT = "/esphome/0156AC/switch/";

    @Test
    void cycle() throws IOException {

        var switchFan = new ESPHomeSwitch(MQTT_BROKER, ESPHOME_SWITCH_TOPIC_ROOT + "t-relay-2-r0-fan");
        var switchRunning = new ESPHomeSwitch(MQTT_BROKER, ESPHOME_SWITCH_TOPIC_ROOT + "t-relay-2-r1-condenser");
        var switchMode = new ESPHomeSwitch(MQTT_BROKER, ESPHOME_SWITCH_TOPIC_ROOT + "t-relay-2-r2-mode");
        var delay = Duration.ofSeconds(1);

        // This should throw a wrench into everything...
        Flux
                .just(true, true, true)
                .delayElements(delay)
                .map(switchMode::setState)
                .blockLast();

        logger.info("set mode to 1");

        try (var hp = new HeatPump(
                "heatpump",
                switchMode, true,
                switchRunning, false,
                switchFan, false,
                Duration.ofSeconds(3),
                null)) {

            var commands = Flux
                    .just(
                            new HvacCommand(HvacMode.COOLING, 0d, 0d),
                            new HvacCommand(null, 1d, 1d),
                            new HvacCommand(HvacMode.HEATING, 0d, 0d),
                            new HvacCommand(null, 1d, 1d),
                            new HvacCommand(null, 0d, 0d))
                    .delayElements(delay)
                    .doOnNext(command -> logger.info("command: {}", command))
                    .map(c -> new Signal<HvacCommand, Void>(Instant.now(), c));

            assertThatCode(() -> {
                        hp
                                .compute(commands)
                                .doOnNext(s -> logger.info("output: {}", s))
                                .doOnNext(s -> {
                                    if (s.isError()) {
                                        throw new IllegalStateException("Failure output received: " + s);
                                    }
                                })
                                .blockLast();

                        logger.info("done");
                    })
                    .doesNotThrowAnyException();

            // Should close() here automatically
        }
    }
}
