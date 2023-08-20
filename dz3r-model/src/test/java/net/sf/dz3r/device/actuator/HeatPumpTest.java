package net.sf.dz3r.device.actuator;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Duration;
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
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isNull();
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Set demand to zero command - actual
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isNull();
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                // This shall not pass...
                .assertNext(e -> {
                    assertThat(e.isError()).isTrue();
                    assertThat(e.error)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageStartingWith("Can't accept demand > 0 before setting the operating mode, signal: ");
                })
                // --
                // ...but this will
                .assertNext(e -> {
                    // Mode change command - requested
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Mode change command - actual
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    // Demand change command - requested
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isEqualTo(0.7);
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Demand change command - actual
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.demand).isEqualTo(0.7);
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isEqualTo(0.7);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    // Shutdown - requested
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isEqualTo(0.7);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Shutdown - actual
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isZero();
                })
                .verifyComplete();
    }

    @Test
    void changeMode() { // NOSONAR It's not complex, it's just mundane

        var switchMode = new NullSwitch("mode");
        var switchRunning = new NullSwitch("running");
        var switchFan = new NullSwitch("fan");

        var d = new HeatPump("hp",
                switchMode, false,
                switchRunning, false,
                switchFan, false,
                Duration.ofMillis(5));
        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(HvacMode.HEATING, 0.8, null)),
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(HvacMode.COOLING, 0.7, null))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                // --
                // Init sequence
                .assertNext(e -> {
                    // ...
                })
                .assertNext(e -> {
                    // ...
                })
                // --
                .assertNext(e -> {
                    // Mode change to heating command, shutting off the condenser - requested
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.HEATING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Mode change to heating command, shutting off the condenser - actual
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.HEATING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    // Setting the demand after the delay - requested
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.HEATING);
                    assertThat(e.getValue().requested.demand).isEqualTo(0.8);
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Setting the demand after the delay - actual
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.HEATING);
                    assertThat(e.getValue().requested.demand).isEqualTo(0.8);
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isEqualTo(0.8);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    // Mode change to cooling command, shutting off the condenser - requested
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    // Still heating...
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.HEATING);
                    // ...but zero demand
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isEqualTo(0.8);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Mode change to cooling command, shutting off the condenser - actual
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    // The actual mode change to cooling command - requested
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // The actual mode change to cooling command - actual
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    // Cooling demand - requested
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isEqualTo(0.7);
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // Cooling demand - actual
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.demand).isEqualTo(0.7);
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isEqualTo(0.7);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                // Shutdown
                .assertNext(e -> {
                    // ...
                })
                .assertNext(e -> {
                    // ...
                })
                .verifyComplete();
    }

    /**
     * Make sure the actual sequence received at boot works as expected.
     */
    @Test
    void boot() { // NOSONAR It's not complex, it's just mundane

        var switchMode = new NullSwitch("mode");
        var switchRunning = new NullSwitch("running");
        var switchFan = new NullSwitch("fan");

        var d = new HeatPump("hp", switchMode, switchRunning, switchFan);
        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(null, 0.0, null)),
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(HvacMode.COOLING, null, null)),
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(null, 1.0, 1.0))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                // --
                // Shut off the condenser
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isNull();
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isNull();
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                // Switch the mode to COOLING
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isNull();
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isNull();
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                // --
                // Start working
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isNull();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isEqualTo(1.0);
                    assertThat(e.getValue().requested.fanSpeed).isEqualTo(1.0);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isEqualTo(1.0);
                    assertThat(e.getValue().requested.fanSpeed).isEqualTo(1.0);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isEqualTo(1.0);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isEqualTo(1.0);
                })
                // --
                // Shut down
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.REQUESTED);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isEqualTo(1.0);
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isEqualTo(1.0);
                })
                .assertNext(e -> {
                    assertThat(e.getValue().kind).isEqualTo(HvacDeviceStatus.Kind.ACTUAL);
                    assertThat(e.getValue().requested.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().requested.demand).isZero();
                    assertThat(e.getValue().requested.fanSpeed).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.demand).isZero();
                    assertThat(((HeatPump.HeatpumpStatus)e.getValue()).actual.fanSpeed).isZero();
                })
                .verifyComplete();
    }
}
