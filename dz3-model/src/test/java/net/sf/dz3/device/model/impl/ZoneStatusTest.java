package net.sf.dz3.device.model.impl;

import net.sf.dz3.device.model.ZoneStatus;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ZoneStatusTest {

    private final Random rg = new Random();

    @Test
    public void testSetpointNaN() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ZoneStatusImpl(Double.NaN, 0, true, true))
                .withMessage("Invalid setpoint NaN");
    }

    @Test
    public void testSetpointPositiveInfinity() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ZoneStatusImpl(Double.POSITIVE_INFINITY, 0, true, true))
                .withMessage("Invalid setpoint Infinity");
    }

    @Test
    public void testSetpointNegativeInfinity() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ZoneStatusImpl(Double.NEGATIVE_INFINITY, 0, true, true))
                .withMessage("Invalid setpoint -Infinity");
    }

    @Test
    public void testNegativeDumpPriority() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ZoneStatusImpl(0, -1, true, true))
                .withMessage("Dump priority must be non-negative (-1 given)");
    }

    @Test
    public void testEqualsNull() {

        assertThat(new ZoneStatusImpl(0, 0, true, true).equals(null))
                .as("null comparison")
                .isFalse();
    }

    public void testEqualsAlien() {

        ZoneStatus zs = new ZoneStatusImpl(0, 0, true, true);

        assertThat(zs.equals(zs.toString())).as("null comparison").isFalse();
    }

    @Test
    public void testEqualsThermostatStatus() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ThermostatStatusImpl(0, 0, 0, true, true, true, false);

        assertThat(a.equals(b)).as("heterogenous comparison").isTrue();
    }

    @Test
    public void testEqualsSame() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, true, true);

        assertThat(a.equals(b)).as("comparison").isTrue();
    }

    @Test
    public void testDifferentSetpoint() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(1, 0, true, true);

        assertThat(a.equals(b)).as("comparison").isFalse();
    }

    @Test
    public void testDifferentDump() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 1, true, true);

        assertThat(a.equals(b)).as("comparison").isFalse();
    }

    @Test
    public void testDifferentEnabled() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, false, true);

        assertThat(a.equals(b)).as("comparison").isFalse();
    }

    @Test
    public void testDifferentVoting() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, true, false);

        assertThat(a.equals(b)).as("comparison").isFalse();
    }

    @Test
    public void testHashCodeEquals() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(0, 0, true, true);

        assertThat(a.hashCode()).as("hashcode comparison").isEqualTo(b.hashCode());
    }

    @Test
    public void testHashCodeDiffers() {

        ZoneStatus a = new ZoneStatusImpl(0, 0, true, true);
        ZoneStatus b = new ZoneStatusImpl(1, 0, true, true);

        assertThat(a.hashCode()).as("hashcode comparison").isNotEqualTo(b.hashCode());
    }

    @Test
    public void testToString0011() {
        assertThat(new ZoneStatusImpl(0, 0, true, true).toString()).as("string representation").isEqualTo("setpoint=0.0, enabled, voting");
    }

    @Test
    public void testToString0111() {
        assertThat(new ZoneStatusImpl(0, 1, true, true).toString()).as("string representation").isEqualTo("setpoint=0.0, enabled, voting, dump priority=1");
    }

    @Test
    public void testToString0101() {
        assertThat(new ZoneStatusImpl(0, 1, false, true).toString()).as("string representation").isEqualTo("setpoint=0.0, disabled, voting, dump priority=1");
    }

    @Test
    public void testToString0110() {
        assertThat(new ZoneStatusImpl(0, 1, true, false).toString()).as("string representation").isEqualTo("setpoint=0.0, enabled, not voting, dump priority=1");
    }
}
