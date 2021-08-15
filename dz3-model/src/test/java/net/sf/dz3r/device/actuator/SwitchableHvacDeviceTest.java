package net.sf.dz3r.device.actuator;

import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.NullSwitch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SwitchableHvacDeviceTest {

    @Test
    void lifecycle() {

        var now = Instant.now();
        var minDelayMillis = 50;
        var maxDelayMillis = 200;
        var s = new NullSwitch("a", minDelayMillis, maxDelayMillis, null);
        var d = new SwitchableHvacDevice("d", HvacMode.COOLING, s);

        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(now, new HvacCommand(null, 0.8, null)),
                new Signal<HvacCommand, Void>(now.plus(300, ChronoUnit.MILLIS), new HvacCommand(null, 0.5, null)),
                new Signal<HvacCommand, Void>(now.plus(2000, ChronoUnit.MILLIS), new HvacCommand(null, 0.0, null))
        );

        var result = d.compute(sequence).log();

        // VT: FIXME: Use virtual time

        StepVerifier
                .create(result)
                .assertNext(e -> {
                    // Actual is not yet set
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.8);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isNull();
                })
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.8);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                // --
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.5);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.5);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                // --
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.0);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.0);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isFalse();
                })
                .verifyComplete();
    }

    /**
     * Make sure that the wrong mode is refused - these devices don't support more than one at a time.
     */
    @Test
    void wrongMode() {

        var d = new SwitchableHvacDevice("d", HvacMode.COOLING, mock(Switch.class));
        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(HvacMode.HEATING, 0.8, null))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                .assertNext(e -> {
                    assertThat(e.isError()).isTrue();
                    assertThat(e.error)
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("Heating is not supported by this instance");
                })
                .verifyComplete();
    }

    /**
     * Make sure that the fan function is not allowed for heat-only devices.
     */
    @Test
    void noFansForHeating() {

        var d = new SwitchableHvacDevice("d", HvacMode.HEATING, mock(Switch.class));
        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(null, 0.8, 1.0))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                .assertNext(e -> {
                    assertThat(e.isError()).isTrue();
                    assertThat(e.error)
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("fanSpeed=1.0 is not supported by this instance (not in cooling mode)");
                })
                .verifyComplete();
    }

    @Test
    void allowFansForCooling() {

        var d = new SwitchableHvacDevice("d", HvacMode.COOLING, mock(Switch.class));
        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(null, 0.8, 1.0))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isEqualTo(1.0);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isNull();
                })
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isEqualTo(1.0);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                .verifyComplete();
    }

    /**
     * Make sure that mode-only command is properly handled.
     */
    @Test
    void modeOnly() {

        var d = new SwitchableHvacDevice("d", HvacMode.COOLING, mock(Switch.class));
        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(HvacMode.COOLING, null, null))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                .verifyComplete();
    }

    /**
     * Make sure that interleaved cool-fan-cool-fan-etc. command sequence is properly supported.
     */
    @Test
    void interleave() {

        var now = Instant.now();
        var minDelayMillis = 50;
        var maxDelayMillis = 200;
        var d = new SwitchableHvacDevice("d", HvacMode.COOLING, mock(Switch.class));

        var sequence = Flux.just(
                // First, request cooling
                new Signal<HvacCommand, Void>(now, new HvacCommand(null, 0.8, null)),
                // Now, request fan on
                new Signal<HvacCommand, Void>(now.plus(300, ChronoUnit.MILLIS), new HvacCommand(null, null, 0.5)),
                // Now request it off - the device must stay on
                new Signal<HvacCommand, Void>(now.plus(600, ChronoUnit.MILLIS), new HvacCommand(null, null, 0.0)),
                // Now request no cooling - everything should shut off
                new Signal<HvacCommand, Void>(now.plus(900, ChronoUnit.MILLIS), new HvacCommand(null, 0.0, null))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                // Device must turn on
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.8);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isNull();
                })
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.8);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                // Device must stay on
                .assertNext(e -> {
                    // Requested demand is the previous value
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.8);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isEqualTo(0.5);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.8);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isEqualTo(0.5);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                // Device must still stay on
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.8);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isZero();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.8);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isZero();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                // Device must shut off
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.0);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isZero();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isTrue();
                })
                .assertNext(e -> {
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.demand).isEqualTo(0.0);
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).requested.fanSpeed).isZero();
                    assertThat(((SwitchableHvacDevice.SwitchStatus)e.getValue()).actual).isFalse();
                })
                .verifyComplete();
    }
}
