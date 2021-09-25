package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class OneWireDriverTest {

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
}
