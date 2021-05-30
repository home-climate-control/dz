package net.sf.dz3.device.actuator.impl;

import net.sf.dz3.device.sensor.impl.NullSwitch;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DamperMultiplexerTest {

    private static final Random rg = new SecureRandom();

    @Test
    void parkDefaultSwitch() throws IOException {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper("d1", s1, 0.5);
        var sd2 = new SwitchDamper("d2", s2, 0.5);
        var dm = new DamperMultiplexer("dm", Set.of(sd1, sd2));

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            dm.park().waitFor();
        }).doesNotThrowAnyException();

        assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState()).isTrue();
        assertThat(s2.getState()).isTrue();
    }

    @Test
    void parkCustomGroup() throws IOException {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper("d1", s1, 0.5);
        var sd2 = new SwitchDamper("d2", s2, 0.5);
        var dm = new DamperMultiplexer("dm", Set.of(sd1, sd2));

        // This is nonsense (read park() code), but let's have some innocent fun
        var parkAt = rg.nextDouble();
        dm.setParkPosition(parkAt);

        assertThatCode(() -> {
            dm.park().waitFor();
        }).doesNotThrowAnyException();

        assertThat(dm.getPosition()).isEqualTo(dm.getParkPosition());

        // ... meanwhile, they park where they want to park

        assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState()).isTrue();
        assertThat(s2.getState()).isTrue();
    }

    @Test
    void parkInvertedSwitch() throws IOException {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper("d1", s1, 0.5);
        var sd2 = new SwitchDamper("d2", s2, 0.5, 1, true);
        var dm = new DamperMultiplexer("dm", Set.of(sd1, sd2));

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            dm.park().waitFor();
        }).doesNotThrowAnyException();

        assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState()).isTrue();
        assertThat(s2.getState()).isFalse();
    }

    @Test
    void parkStraightCustomSwitch() throws IOException {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper("d1", s1, 0.5);
        var sd2 = new SwitchDamper("d2", s2, 0.5, 0);
        var dm = new DamperMultiplexer("dm", Set.of(sd1, sd2));

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            dm.park().waitFor();
        }).doesNotThrowAnyException();

        assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState()).isTrue();
        assertThat(s2.getState()).isFalse();
    }

    @Test
    void parkInvertedCustomSwitch() throws IOException {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper("d1", s1, 0.5);
        var sd2 = new SwitchDamper("d2", s2, 0.5, 0, true);
        var dm = new DamperMultiplexer("dm", Set.of(sd1, sd2));

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            dm.park().waitFor();
        }).doesNotThrowAnyException();

        assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState()).isTrue();
        assertThat(s2.getState()).isTrue();
    }

    @Test
    void setPosition() throws IOException {

        var threshold = 0.2 + rg.nextDouble() * 0.6;
        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper("d1", s1, threshold);
        var sd2 = new SwitchDamper("d2", s2, threshold);
        var dm = new DamperMultiplexer("dm", Set.of(sd1, sd2));

        for (var counter = 50; counter > 0; counter--) {

            var position = rg.nextDouble();
            var state = position > threshold;

            dm.set(position);

            assertThat(dm.getPosition()).isEqualTo(position);

            assertThat(sd1.getPosition()).isEqualTo(position);
            assertThat(sd2.getPosition()).isEqualTo(position);

            assertThat(s1.getState()).isEqualTo(state);
            assertThat(s2.getState()).isEqualTo(state);
        }
    }
}
