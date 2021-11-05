package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.test.TestVariableProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class OneWireDriverTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void presenceAsBoolean() {

        var driver = new OneWireDriver(null, DSPortAdapter.Speed.REGULAR);
        var presenceSignal = Flux.concat(driver.isPresent("does not exist"));

        StepVerifier
                .create(presenceSignal)
                .assertNext(s -> assertThat(s).isFalse())
                .verifyComplete();
    }

    @Test
    void presenceAsSignal() {

        var driver = new OneWireDriver(null, DSPortAdapter.Speed.REGULAR);
        var presenceSignal = Flux.concat(driver.checkPresence("does not exist"));

        StepVerifier
                .create(presenceSignal)
                .assertNext(s -> {
                    assertThat(s.isError()).isTrue();
                    assertThat(s.status).isEqualTo(Signal.Status.FAILURE_TOTAL);
                    assertThat(s.error).isInstanceOf(IllegalArgumentException.class).hasMessage("does not exist: not present");
                })
                .verifyComplete();
    }

    @Test
    @Disabled("Enable only if safe to use hardware is connected")
    void getSensorFlux() throws Exception {

        var port = TestVariableProvider.getEnv("ONEWIRE_TEST_PORT", "serial port with safe 1-Wire adapter connected");
        var remoteAddress = "C100080021E4D610";

        try (var xbee = new OneWireDriver(port, DSPortAdapter.Speed.REGULAR)) {
            // VT: NOTE: This will only work with a remote XBee configured to broadcast IS packets
            var signalFlux = xbee
                    .getFlux(remoteAddress)
                    .log()
                    .doOnNext(signal -> logger.info("Signal: {}", signal));

            var stream = signalFlux.take(2).collectList().block();

            var notPresent = stream.get(0);
            var present = stream.get(1);

            AssertionsForClassTypes.assertThat(notPresent.isError()).isTrue();
            AssertionsForClassTypes.assertThat(present.isError()).isFalse();
        }
    }
}
