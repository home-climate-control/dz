package net.sf.dz3r.device.actuator;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
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

    /**
     * Verify that empty command sequence is executed (implementation will issue initialization and shutdown commands).
     */
    @Test
    void empty() { // NOSONAR It's not complex, it's just mundane

        var switchMode = new NullSwitch("mode");
        var switchRunning = new NullSwitch("running");
        var switchFan = new NullSwitch("fan");

        var d = new HeatPump("hp-empty",
                switchMode, false,
                switchRunning, false,
                switchFan, false,
                Duration.ofSeconds(1));
        Flux<Signal<HvacCommand, Void>> sequence = Flux.empty();

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                // --
                // Init sequence
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isNull();
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // Shutdown sequence
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isNull();
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isZero();
                })
                .verifyComplete();
    }

    /**
     * Verify that an attempt to set non-zero demand before the mode is set fails.
     */
    @Test
    void demandBeforeMode() { // NOSONAR It's not complex, it's just mundane

        var switchMode = new NullSwitch("mode");
        var switchRunning = new NullSwitch("running");
        var switchFan = new NullSwitch("fan");

        var d = new HeatPump("hp-initial-mode",
                switchMode, false,
                switchRunning, false,
                switchFan, false,
                Duration.ofSeconds(1));
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
                    assertThat(e.getValue().command.mode).isNull();
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // This shall not pass...
                .assertNext(e -> {
                    assertThat(e.isError()).isTrue();
                    assertThat(e.error)
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageStartingWith("Demand command issued before mode is set (likely programming error)");
                })
                // --
                // ...but this will
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isEqualTo(0.7);
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // Demand change command - requested
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isEqualTo(0.7);
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // Shutdown sequence
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isZero();
                })
                .verifyComplete();
    }

    /**
     * Verify that a single mode change command executes as expected.
     */
    @Test
    void setMode() { // NOSONAR It's not complex, it's just mundane

        var switchMode = new NullSwitch("mode");
        var switchRunning = new NullSwitch("running");
        var switchFan = new NullSwitch("fan");

        var d = new HeatPump("hp-change-mode",
                switchMode, false,
                switchRunning, false,
                switchFan, false,
                Duration.ofSeconds(1));
        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(HvacMode.HEATING, 0.8, null))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                // --
                // Init sequence
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isNull();
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // Set mode to HEATING
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.HEATING);
                    assertThat(e.getValue().command.demand).isEqualTo(0.8);
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // Turn on the condenser
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.HEATING);
                    assertThat(e.getValue().command.demand).isEqualTo(0.8);
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // Shutdown sequence
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.HEATING);
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isZero();
                })
                .verifyComplete();
    }

    @Test
    void changeMode() { // NOSONAR It's not complex, it's just mundane

        var switchMode = new NullSwitch("mode");
        var switchRunning = new NullSwitch("running");
        var switchFan = new NullSwitch("fan");

        var d = new HeatPump("hp-change-mode",
                switchMode, false,
                switchRunning, false,
                switchFan, false,
                Duration.ofSeconds(1));
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
                    assertThat(e.getValue().command.mode).isNull();
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // (heating, 0.8, null)
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.HEATING);
                    assertThat(e.getValue().command.demand).isEqualTo(0.8);
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // VT: FIXME: Why twice?
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.HEATING);
                    assertThat(e.getValue().command.demand).isEqualTo(0.8);
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // (cooling, 0.7, null)
                .assertNext(e -> {
                    // Mode change to cooling command, shutting off the condenser - requested
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    // ... and set the demand
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isEqualTo(0.7);
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                .assertNext(e -> {
                    // VT: FIXME: Why twice?
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isEqualTo(0.7);
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // Shutdown sequence
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isZero();
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

        var d = new HeatPump("hp-boot",
                switchMode, false,
                switchRunning, false,
                switchFan, false,
                Duration.ofSeconds(1));
        var sequence = Flux.just(
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(null, 0d, null)),
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(HvacMode.COOLING, null, null)),
                new Signal<HvacCommand, Void>(Instant.now(), new HvacCommand(null, 1d, 1d))
        );

        var result = d.compute(sequence).log();

        StepVerifier
                .create(result)
                // --
                // Init sequence
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isNull();
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // (null, 0d, null)
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isNull();
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                // (cooling, null, null) => (cooling, 0d, null) because reconcile()
                // Switch the mode to COOLING without delays
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // VT: FIXME: Why twice?
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isNull();
                })
                // --
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isEqualTo(1.0);
                    assertThat(e.getValue().command.fanSpeed).isEqualTo(1.0);
                })
                // --
                // Shutdown sequence
                .assertNext(e -> {
                    assertThat(e.getValue().command.mode).isEqualTo(HvacMode.COOLING);
                    assertThat(e.getValue().command.demand).isZero();
                    assertThat(e.getValue().command.fanSpeed).isZero();
                })
                .verifyComplete();
    }
}
