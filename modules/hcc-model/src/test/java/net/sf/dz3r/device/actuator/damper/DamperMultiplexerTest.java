package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.device.actuator.NullSwitch;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DamperMultiplexerTest {

    private static final Random rg = new SecureRandom();

    @Test
    void parkDefaultSwitch() {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper<>("d1", s1, 0.5);
        var sd2 = new SwitchDamper<>("d2", s2, 0.5);
        var dm = new DamperMultiplexer<>("dm", Set.of(sd1, sd2));

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            dm.park().block();
        }).doesNotThrowAnyException();

        // Can't access damper position in reactive implementation
        // assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        // assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState().block()).isTrue();
        assertThat(s2.getState().block()).isTrue();
    }

    @Test
    void parkInvertedSwitch() {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper<>("d1", s1, 0.5);
        var sd2 = new SwitchDamper<>("d2", s2, 0.5, 1.0, true);
        var dm = new DamperMultiplexer<>("dm", Set.of(sd1, sd2));

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            dm.park().block();
        }).doesNotThrowAnyException();

        // Can't access damper position in reactive implementation
        // assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        // assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState().block()).isTrue();
        assertThat(s2.getState().block()).isFalse();
    }

    @Test
    void parkCustomGroup() {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper<>("d1", s1, 0.5);
        var sd2 = new SwitchDamper<>("d2", s2, 0.5);

        // This is nonsense (read park() code), but let's have some innocent fun
        var parkAt = rg.nextDouble();
        var dm = new DamperMultiplexer<>("dm", Set.of(sd1, sd2), parkAt);

        assertThatCode(() -> dm.park().block()).doesNotThrowAnyException();

        // Can't access damper position in reactive implementation
        // assertThat(dm.getPosition()).isEqualTo(dm.getParkPosition());

        // ... meanwhile, they park where they want to park

        // assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        // assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState().block()).isTrue();
        assertThat(s2.getState().block()).isTrue();
    }

    @Test
    void parkStraightCustomSwitch() {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper<>("d1", s1, 0.5);
        var sd2 = new SwitchDamper<>("d2", s2, 0.5, 0.0);
        var dm = new DamperMultiplexer<>("dm", Set.of(sd1, sd2));

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            dm.park().block();
        }).doesNotThrowAnyException();

        // Can't access damper position in reactive implementation
        // assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        // assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState().block()).isTrue();
        assertThat(s2.getState().block()).isFalse();
    }

    @Test
    void parkInvertedCustomSwitch() {

        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper<>("d1", s1, 0.5);
        var sd2 = new SwitchDamper<>("d2", s2, 0.5, 0.0, true);
        var dm = new DamperMultiplexer<>("dm", Set.of(sd1, sd2));

        assertThatCode(() -> {
            // Parking position hasn't been explicitly set
            dm.park().block();
        }).doesNotThrowAnyException();

        // Can't access damper position in reactive implementation
        // assertThat(sd1.getPosition()).isEqualTo(sd1.getParkPosition());
        // assertThat(sd2.getPosition()).isEqualTo(sd2.getParkPosition());

        assertThat(s1.getState().block()).isTrue();
        assertThat(s2.getState().block()).isTrue();
    }

    @Test
    void setPositionSwitchDamper() {

        var threshold = 0.2 + rg.nextDouble() * 0.6;
        NullSwitch s1 = new NullSwitch("s1");
        NullSwitch s2 = new NullSwitch("s2");
        var sd1 = new SwitchDamper<>("d1", s1, threshold);
        var sd2 = new SwitchDamper<>("d2", s2, threshold);
        var dm = new DamperMultiplexer<>("dm", Set.of(sd1, sd2));

        for (var counter = 50; counter > 0; counter--) {

            var position = rg.nextDouble();
            var state = position > threshold;

            dm.set(position).block();

            // Can't access damper position in reactive implementation
            // assertThat(dm.getPosition()).isEqualTo(position);

            // assertThat(sd1.getPosition()).isEqualTo(position);
            // assertThat(sd2.getPosition()).isEqualTo(position);

            assertThat(s1.getState().block()).isEqualTo(state);
            assertThat(s2.getState().block()).isEqualTo(state);
        }
    }

    @Test
    void setPositionNullDamper() {

        var sd1 = new NullDamper("d1");
        var sd2 = new NullDamper("d2");
        var dm = new DamperMultiplexer<>("dm", Set.of(sd1, sd2));

        for (var counter = 50; counter > 0; counter--) {

            var position = rg.nextDouble();

            dm.set(position).block();

            // Can't access damper position in reactive implementation...
            // assertThat(dm.getPosition()).isEqualTo(position);

            // ...unless it is a NullDamper
             assertThat(sd1.get()).isEqualTo(position);
             assertThat(sd2.get()).isEqualTo(position);
        }
    }
}
