package net.sf.dz3.device.model.impl;

import net.sf.dz3.device.model.ZoneStatus;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ZoneStatusTest {

    private final Random rg = new Random();

    @Test
    void testSetpointNaN() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ZoneStatusImpl(Double.NaN, 0, true, true))
                .withMessage("Invalid setpoint NaN");
    }

    @Test
    void testSetpointPositiveInfinity() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ZoneStatusImpl(Double.POSITIVE_INFINITY, 0, true, true))
                .withMessage("Invalid setpoint Infinity");
    }

    @Test
    void testSetpointNegativeInfinity() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ZoneStatusImpl(Double.NEGATIVE_INFINITY, 0, true, true))
                .withMessage("Invalid setpoint -Infinity");
    }

    @Test
    void testNegativeDumpPriority() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ZoneStatusImpl(0, -1, true, true))
                .withMessage("Dump priority must be non-negative (-1 given)");
    }

    @Test
    void testEqualsNull() {

        assertThat(new ZoneStatusImpl(0, 0, true, true).equals(null))
                .as("null comparison")
                .isFalse();
    }

    void testEqualsAlien() {

        ZoneStatus zs = new ZoneStatusImpl(0, 0, true, true);

        assertThat(zs.equals(zs.toString())).as("null comparison").isFalse();
    }

    @Test
    void testEqualsThermostatStatus() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ThermostatStatusImpl("zone",0, 0, 0, true, true, true, false);

        assertThat(a.equals(b)).as("heterogenous comparison").isTrue();
    }

    @Test
    void testEqualsSame() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, true, true);

        assertThat(a.equals(b)).as("comparison").isTrue();
    }

    @Test
    void testDifferentSetpoint() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(1, 0, true, true);

        assertThat(a.equals(b)).as("comparison").isFalse();
    }

    @Test
    void testDifferentDump() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 1, true, true);

        assertThat(a.equals(b)).as("comparison").isFalse();
    }

    @Test
    void testDifferentEnabled() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, false, true);

        assertThat(a.equals(b)).as("comparison").isFalse();
    }

    @Test
    void testDifferentVoting() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, true, false);

        assertThat(a.equals(b)).as("comparison").isFalse();
    }

    @Test
    void testHashCodeEquals() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, true, true);

        assertThat(a.hashCode()).as("hashcode comparison").isEqualTo(b.hashCode());
    }

    @Test
    void testHashCodeDiffers() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(1, 0, true, true);

        assertThat(a.hashCode()).as("hashcode comparison").isNotEqualTo(b.hashCode());
    }

    @Test
    void testToString0011() {
        assertThat(new ZoneStatusImpl(0, 0, true, true).toString()).as("string representation").isEqualTo("setpoint=0.0, enabled, voting");
    }

    @Test
    void testToString0111() {
        assertThat(new ZoneStatusImpl(0, 1, true, true).toString()).as("string representation").isEqualTo("setpoint=0.0, enabled, voting, dump priority=1");
    }

    @Test
    void testToString0101() {
        assertThat(new ZoneStatusImpl(0, 1, false, true).toString()).as("string representation").isEqualTo("setpoint=0.0, disabled, voting, dump priority=1");
    }

    @Test
    void testToString0110() {
        assertThat(new ZoneStatusImpl(0, 1, true, false).toString()).as("string representation").isEqualTo("setpoint=0.0, enabled, not voting, dump priority=1");
    }
}
