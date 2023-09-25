package net.sf.dz3r.device.xbee;

import net.sf.dz3r.test.TestVariableProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.tools.agent.ReactorDebugAgent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class XBeeDriverTest {

    private final Logger logger = LogManager.getLogger();

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    @Disabled("Enable only if safe to use hardware is connected")
    void getSensorFlux() throws Exception {

        var port = TestVariableProvider.getEnv("XBEE_COORDINATOR_TEST_PORT", "serial port with safe XBee coordinator connected");
        var remoteAddress = "0013A200.402D52DD:A3";

        try (var xbee = new XBeeDriver(port)) {
            // VT: NOTE: This will only work with a remote XBee configured to broadcast IS packets
            var signalFlux = xbee
                    .getFlux(remoteAddress)
                    .log()
                    .doOnNext(signal -> logger.info("Signal: {}", signal));

            var stream = signalFlux.take(2).collectList().block();

            var notPresent = stream.get(0);
            var present = stream.get(1);

            assertThat(notPresent.isError()).isTrue();
            assertThat(present.isError()).isFalse();
        }
    }
}
