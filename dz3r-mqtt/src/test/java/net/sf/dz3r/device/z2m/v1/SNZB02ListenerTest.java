package net.sf.dz3r.device.z2m.v1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.tools.agent.ReactorDebugAgent;

@EnabledIfEnvironmentVariable(
        named = "TEST_DZ_SNZB02",
        matches = "safe",
        disabledReason = "Only execute this test if a suitable MQTT broker and SNZB-02 device are available"
)
class SNZB02ListenerTest {
    private final Logger logger = LogManager.getLogger();

    private final String MQTT_BROKER = "mqtt-zigbee";

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    /**
     * Make sure 3 measurements can be taken normally.
     *
     * VT: NOTE: This might take a long time, Zigbee devices are not talkative.
     */
    @Test
    void take3() {

        var listener = new Z2MJsonListener(MQTT_BROKER, "zigbee2mqtt", "temperature");

        listener
                .getFlux("SNZB-02-00")
                .take(3)
                .doOnNext(signal -> logger.info("signal: {}", signal))
                .blockLast();
    }
}
