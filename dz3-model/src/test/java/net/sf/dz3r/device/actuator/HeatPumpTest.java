package net.sf.dz3r.device.actuator;

import net.sf.dz3.device.sensor.impl.NullSwitch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HeatPumpTest {

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    void initialMode() { // NOSONAR It's not complex, it's just mundane

        var switchMode = new NullSwitch("mode");
        var switchRunning = new NullSwitch("running");
        var switchFan = new NullSwitch("fan");

        var d = new HeatPump("hp", switchMode, switchRunning, switchFan);
        var sequence = Flux.just(
                // This will fail
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(null, 0.8, null)),
                // This will succeed
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(HvacMode.COOLING, 0.7, null))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                // --
                // Init sequence
                .assertNext(e -> {
                    // Set demand to zero command - requested
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).kind).isEqualTo(AbstractHvacDevice.AbstractHvacDeviceStatus.Kind.REQUESTED);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.mode).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Set demand to zero command - actual
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).kind).isEqualTo(AbstractHvacDevice.AbstractHvacDeviceStatus.Kind.ACTUAL);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.mode).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                // This shall not pass...
                .assertNext(e -> {
                    assertThat(e.isError()).isTrue();
                    assertThat(e.error)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("Can't accept demand > 0 before setting the operating mode");
                })
                // --
                // ...but this will
                .assertNext(e -> {
                    // Mode change command - requested
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).kind).isEqualTo(AbstractHvacDevice.AbstractHvacDeviceStatus.Kind.REQUESTED);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Mode change command - actual
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).kind).isEqualTo(AbstractHvacDevice.AbstractHvacDeviceStatus.Kind.ACTUAL);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    // Demand change command - requested
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).kind).isEqualTo(AbstractHvacDevice.AbstractHvacDeviceStatus.Kind.REQUESTED);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.demand).isEqualTo(0.7);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Demand change command - actual
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).kind).isEqualTo(AbstractHvacDevice.AbstractHvacDeviceStatus.Kind.ACTUAL);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.demand).isEqualTo(0.7);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isEqualTo(0.7);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    // Shutdown - requested
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).kind).isEqualTo(AbstractHvacDevice.AbstractHvacDeviceStatus.Kind.REQUESTED);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.fanSpeed).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isEqualTo(0.7);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Shutdown - actual
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).kind).isEqualTo(AbstractHvacDevice.AbstractHvacDeviceStatus.Kind.ACTUAL);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).requested.fanSpeed).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isZero();
                })
                .verifyComplete();
    }
}
