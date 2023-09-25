package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.device.actuator.NullSwitch;
import net.sf.dz3r.device.actuator.Switch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class SwitchDamperTest {

    private final Logger logger = LogManager.getLogger();
    private final Random rg = new SecureRandom();

    @Test
    void validName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SwitchDamper<>(null, null, 0d))
                .withMessage("address can't be null");
    }

    @Test
    void validTarget() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SwitchDamper<>("name", null, 0d))
                .withMessage("target can't be null");
    }

    @Test
    void constructor1() throws IOException {
        assertThatCode(() -> {
            new SwitchDamper<>("damper", new NullSwitch("switch"), 0.5, 0.1);
        }).doesNotThrowAnyException();
    }

    @Test
    void constructor2() throws IOException {
        assertThatCode(() -> {
            new SwitchDamper<>("damper", new NullSwitch("switch"), 0.5, 0.1, true);
        }).doesNotThrowAnyException();
    }

    @Test
    void parkDefault() throws IOException, InterruptedException {

        var s = new NullSwitch("switch");
        var d = new SwitchDamper<>("damper", s, 0.5);

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            var position = d.park().block();
            assertThat(position).isEqualTo(Damper.DEFAULT_PARK_POSITION);
        }).doesNotThrowAnyException();

        // Can't assess the damper position (there is none), but switch state is known
        assertThat(s.getState().block()).isTrue();
    }

    /**
     * Test whether the {@link SwitchDamper} is properly parked.
     */
    @Test
    void park() {

        NullSwitch s = new NullSwitch("switch");
        var d = new SwitchDamper<>("damper", s, 0.5);

        try {

            // Test with default parked position first
            testPark(d, null);

            for (double parkedPosition = 0.9; parkedPosition > 0.05; parkedPosition -= 0.1) {
                testPark(d, parkedPosition);
            }

        } catch (Throwable t) {

            logger.fatal("Oops", t);
            fail(t.getMessage());
        }
    }

    /**
     * Test if the damper is properly parked in a given position.
     *
     * @param target Damper to test.
     * @param parkedPosition Position to park in.
     */
    private void testPark(Damper<?> target, Double parkedPosition) throws IOException, InterruptedException {

        if (parkedPosition != null) {

            target.setParkPosition(parkedPosition);
            assertThat(target.getParkPosition()).as("parked position").isEqualTo(parkedPosition);
        }

        logger.info("park position: " + parkedPosition);

        Flux.just(0, 1, 0)
                .doOnNext(target::set)
                .blockLast();

        var parkedAt = target.park().block();

        assertThat(parkedAt).as("parked position").isEqualTo(target.getParkPosition());
    }

    @Test
    void thresholdGood() {
        assertThatCode(() -> {
            var s = mock(Switch.class);
            new SwitchDamper<>("sd", s, rg.nextDouble());
        }).doesNotThrowAnyException();
    }

    @Test
    void thresholdBad() {

        var s = mock(Switch.class);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SwitchDamper<>("sd", s, -1.0))
                .withMessage("Unreasonable threshold value given (-1.0), valid values are (0 < threshold < 1)");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SwitchDamper<>("sd", s, 2.0))
                .withMessage("Unreasonable threshold value given (2.0), valid values are (0 < threshold < 1)");
    }

    @Test
    void inverted() throws IOException {

        var s = new NullSwitch("switch");
        var d = new SwitchDamper<>("sd", s, 0.5, 1.0, true);

        d.park().block();

        // Parked at 1.0, inverted
        assertThat(s.getState().block()).isFalse();

        d.set(0).block();
        assertThat(s.getState().block()).isTrue();

        d.set(1).block();
        assertThat(s.getState().block()).isFalse();
    }
}
