package net.sf.dz3r.device.zwave.v2;

import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import net.sf.dz3r.device.mqtt.v2rx.MqttAdapterImpl;
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
        named = "TEST_HCC_ZWAVE_SWITCH",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker and Z-Wave switch device are available"
)
class ZWaveCqrsBinarySwitchTest {

    private final Logger logger = LogManager.getLogger();

    private final String MQTT_BROKER = "mqtt-zwave";

    private final String ZWAVE_SWITCH_TOPIC = "zwave/SE_Bedroom/MP31ZP-0";

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    void setState1010() {

        assertThatCode(() -> {

            try (var adapter = new MqttAdapterImpl(new MqttEndpoint(MQTT_BROKER))) {

                var zwaveSwitch = new ZWaveCqrsBinarySwitch(
                        "zwave",
                        Clock.systemUTC(),
                        null, null,
                        adapter,
                        ZWAVE_SWITCH_TOPIC
                );

                // VT: NOTE: This switch doesn't control anything critical now, does it?

                var send = Flux
                        .just(true, false, true, false)
                        .delayElements(Duration.ofSeconds(1))
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(state -> {
                            logger.info("Switch state requested={}", state);
                            zwaveSwitch.setState(state);
                        });

                // This is likely to miss the last status update; good enough for now
                zwaveSwitch
                        .getFlux()
                        .subscribe(s -> logger.info("status: {}", s));

                send.blockLast();

                logger.info("DONE");
            }

        }).doesNotThrowAnyException();
    }
}
