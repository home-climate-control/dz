package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.hcc.model.HvacMode;
import com.homeclimatecontrol.hcc.signal.hvac.HvacCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ReconcilerTest {

    private final HeatPump.Reconciler reconciler = new HeatPump.Reconciler();

    /**
     * Fail.
     */
    @Test
    void nullToDemand() {

        var prev = new HvacCommand(null, null, null);
        var next = new HvacCommand(null, 1d, null);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    reconciler.reconcile("null-to-demand", prev, next);
                });
    }

    @Test
    void modeToNull() {

        var prev = new HvacCommand(HvacMode.HEATING, 1d, null);
        var next = new HvacCommand(null, 1d, null);
        var result = reconciler.reconcile("mode-to-null", prev, next);

        assertThat(result.command().mode()).isEqualTo(HvacMode.HEATING);
        assertThat(result.command().demand()).isEqualTo(1d);
        assertThat(result.command().fanSpeed()).isNull();
        assertThat(result.modeChangeRequired()).isFalse();
        assertThat(result.delayRequired()).isFalse();
    }

    /**
     * Mode change is required, delay is not.
     */
    @Test
    void nullToHeating() {

        var prev = new HvacCommand(null, null, null);
        var next = new HvacCommand(HvacMode.HEATING, 1d, null);
        var result = reconciler.reconcile("null-to-heating", prev, next);

        assertThat(result.command().mode()).isEqualTo(HvacMode.HEATING);
        assertThat(result.command().demand()).isEqualTo(1d);
        assertThat(result.command().fanSpeed()).isNull();
        assertThat(result.modeChangeRequired()).isTrue();
        assertThat(result.delayRequired()).isFalse();
    }

    /**
     * Both mode change and delay are required.
     */
    @Test
    void coolingToHeating() {

        var prev = new HvacCommand(HvacMode.COOLING, null, null);
        var next = new HvacCommand(HvacMode.HEATING, 1d, null);
        var result = reconciler.reconcile("cooling-to-heating", prev, next);

        assertThat(result.command().mode()).isEqualTo(HvacMode.HEATING);
        assertThat(result.command().demand()).isEqualTo(1d);
        assertThat(result.command().fanSpeed()).isNull();
        assertThat(result.modeChangeRequired()).isTrue();
        assertThat(result.delayRequired()).isTrue();
    }

    /**
     * Fan by itself is OK.
     */
    @Test
    void nullToFan() {

        var prev = new HvacCommand(null, null, null);
        var next = new HvacCommand(null, null, 1d);
        var result = reconciler.reconcile("null-to-fan", prev, next);

        assertThat(result.command().mode()).isNull();
        assertThat(result.command().demand()).isNull();
        assertThat(result.command().fanSpeed()).isEqualTo(1d);
        assertThat(result.modeChangeRequired()).isFalse();
        assertThat(result.delayRequired()).isFalse();
    }

    /**
     * Turning off the fan by itself is also OK.
     */
    @Test
    void fanToZero() {

        var prev = new HvacCommand(null, null, 1d);
        var next = new HvacCommand(null, null, 0d);
        var result = reconciler.reconcile("fan-to-zero", prev, next);

        assertThat(result.command().mode()).isNull();
        assertThat(result.command().demand()).isNull();
        assertThat(result.command().fanSpeed()).isEqualTo(0d);
        assertThat(result.modeChangeRequired()).isFalse();
        assertThat(result.delayRequired()).isFalse();
    }

    /**
     * Null means "stay where you were", not "0".
     */
    @Test
    void anythingToNull() {

        var prev = new HvacCommand(HvacMode.HEATING, 1d, 1d);
        var next = new HvacCommand(null, null, null);
        var result = reconciler.reconcile("anything-to-null", prev, next);

        assertThat(result.command().mode()).isEqualTo(HvacMode.HEATING);
        assertThat(result.command().demand()).isEqualTo(1d);
        assertThat(result.command().fanSpeed()).isEqualTo(1d);
        assertThat(result.modeChangeRequired()).isFalse();
        assertThat(result.delayRequired()).isFalse();
    }
}
