package net.sf.dz3r.device.actuator;

import net.sf.dz3.device.sensor.impl.NullSwitch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SwitchableHvacDeviceTest {

    @Test
    @Disabled("Needs more work, see FIXME")
    void lifecycle() {

        var now = Instant.now();
        var minDelayMillis = 50;
        var maxDelayMillis = 200;
        var s = new NullSwitch("a", minDelayMillis, maxDelayMillis, null);
        var d = new SwitchableHvacDevice("d", HvacMode.COOLING, s);

        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(now, new HvacCommand(null, 0.8, null))
                ,
                new Signal<HvacCommand, Void>(now.plus(300, ChronoUnit.MILLIS), new HvacCommand(null, 0.5, null)),
                new Signal<HvacCommand, Void>(now.plus(2000, ChronoUnit.MILLIS), new HvacCommand(null, 0.0, null))
        );

        var result = d.compute(sequence).log();

        // VT: FIXME: Termination condition?
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
                    // Actual is not yet set
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
                    // Actual is not yet set
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
}
