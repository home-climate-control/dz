package net.sf.dz3r.device.z2m.v2;

import net.sf.dz3r.device.mqtt.v1.MqttAdapterImpl;
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
        named = "TEST_HCC_Z2M_SWITCH",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker and Zigbee switch device are available"
)
class Z2MCqrsSwitchTest {

    private final Logger logger = LogManager.getLogger();

    private final String MQTT_BROKER = "mqtt-zigbee";

    private final String ZIGBEE_SWITCH_TOPIC = "zigbee2mqtt-dev/sengled-01";

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    void setState1010() {

        assertThatCode(() -> {

            try (var adapter = new MqttAdapterImpl(new MqttEndpoint(MQTT_BROKER))) {

                var z2mSwitch = new Z2MCqrsSwitch(
                        "zigbee",
                        Clock.systemUTC(),
                        null, null,
                        adapter,
                        ZIGBEE_SWITCH_TOPIC
                );

                // VT: NOTE: This switch doesn't control anything critical now, does it?

                var send = Flux
                        .just(true, false, true, false)
                        .delayElements(Duration.ofSeconds(1))
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(state -> {
                            logger.info("Switch state requested={}", state);
                            z2mSwitch.setState(state);
                        });

                // This is likely to miss the last status update; good enough for now
                z2mSwitch
                        .getFlux()
                        .subscribe(s -> logger.info("status: {}", s));

                send.blockLast();

                logger.info("DONE");
            }

        }).doesNotThrowAnyException();
    }
}
