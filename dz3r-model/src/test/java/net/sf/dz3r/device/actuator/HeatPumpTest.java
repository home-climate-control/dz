package net.sf.dz3r.device.actuator;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HeatPumpTest {

    private final Logger logger = LogManager.getLogger();
    private final Duration delay = Duration.ofMillis(500);

    private final Scheduler scheduler = Schedulers.newSingle("heatpump-test-single");

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    private SwitchPack getSwitchPack() {

        // VT: NOTE: Might need to use this for running parameterized tests with different schedulers
        return new SwitchPack(
                new NullSwitch("mode", scheduler),
                new NullSwitch("running", scheduler),
                new NullSwitch("fan", scheduler)
        );
    }

    /**
     * Verify that empty command sequence is executed (implementation will issue initialization and shutdown commands).
     */
    @Test
    void empty() { // NOSONAR It's not complex, it's just mundane

        var switchPack = getSwitchPack();
        var d = new HeatPump("hp-empty",
                switchPack.mode, false,
                switchPack.running, false,
                switchPack.fan, false,
                delay,
                scheduler);
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

        var switchPack = getSwitchPack();
        var d = new HeatPump("hp-initial-mode",
                switchPack.mode, false,
                switchPack.running, false,
                switchPack.fan, false,
                delay,
                scheduler);
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

        var switchPack = getSwitchPack();
        var d = new HeatPump("hp-change-mode",
                switchPack.mode, false,
                switchPack.running, false,
                switchPack.fan, false,
                delay,
                scheduler);
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

        var switchPack = getSwitchPack();
        var d = new HeatPump("hp-change-mode",
                switchPack.mode, false,
                switchPack.running, false,
                switchPack.fan, false,
                delay,
                scheduler);
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

        var switchPack = getSwitchPack();
        var d = new HeatPump("hp-boot",
                switchPack.mode, false,
                switchPack.running, false,
                switchPack.fan, false,
                delay,
                scheduler);
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

    @Test
    void delayElementsFromFlux1() {

        var s = Schedulers.newSingle("single");
        var head = Flux.range(0, 3).publishOn(s);
        var detour = Flux.just(42).delayElements(delay, s);
        var tail = Flux.range(3, 3);

        var result = Flux.concat(
                        head,
                        detour.flatMap(Flux::just),
                        tail)
                .doOnNext(e -> logger.info("{}", e));

        StepVerifier
                .create(result)
                .assertNext(e -> assertThat(e).isZero())
                .assertNext(e -> assertThat(e).isEqualTo(1))
                .assertNext(e -> assertThat(e).isEqualTo(2))
                .assertNext(e -> assertThat(e).isEqualTo(42))
                .assertNext(e -> assertThat(e).isEqualTo(3))
                .assertNext(e -> assertThat(e).isEqualTo(4))
                .assertNext(e -> assertThat(e).isEqualTo(5))
                .verifyComplete();
    }

    @Test
    void delayElementsFromFlux2() {

        var s = Schedulers.newSingle("single");
        var head = Flux.range(0, 3).publishOn(s);
        var detour = Flux.just(42).delayElements(delay);
        var tail = Flux.range(3, 3);

        var result = Flux.concat(
                        head,
                        detour.flatMap(Flux::just).publishOn(s),
                        tail)
                .doOnNext(e -> logger.info("{}", e));

        StepVerifier
                .create(result)
                .assertNext(e -> assertThat(e).isZero())
                .assertNext(e -> assertThat(e).isEqualTo(1))
                .assertNext(e -> assertThat(e).isEqualTo(2))
                .assertNext(e -> assertThat(e).isEqualTo(42))
                .assertNext(e -> assertThat(e).isEqualTo(3))
                .assertNext(e -> assertThat(e).isEqualTo(4))
                .assertNext(e -> assertThat(e).isEqualTo(5))
                .verifyComplete();
    }

    @Test
    void delayElementsFromMono1() {

        var s = Schedulers.newSingle("single");
        var head = Flux.range(0, 3).publishOn(s);
        var detour = Flux.just(-1).flatMap(ignore -> Mono.just(42)).delayElements(delay, s);
        var tail = Flux.range(3, 3);

        var result = Flux.concat(head, detour, tail)
                .doOnNext(e -> logger.info("{}", e));

        StepVerifier
                .create(result)
                .assertNext(e -> assertThat(e).isZero())
                .assertNext(e -> assertThat(e).isEqualTo(1))
                .assertNext(e -> assertThat(e).isEqualTo(2))
                .assertNext(e -> assertThat(e).isEqualTo(42))
                .assertNext(e -> assertThat(e).isEqualTo(3))
                .assertNext(e -> assertThat(e).isEqualTo(4))
                .assertNext(e -> assertThat(e).isEqualTo(5))
                .verifyComplete();
    }

    @Test
    void delayElementsFromMono2() {

        var s = Schedulers.newSingle("single");
        var head = Flux.range(0, 3).publishOn(s);
        var detour = Flux.just(-1).flatMap(ignore -> Mono.just(42).delayElement(delay)).publishOn(s);
        var tail = Flux.range(3, 3);

        var result = Flux.concat(head, detour, tail)
                .doOnNext(e -> logger.info("{}", e));

        StepVerifier
                .create(result)
                .assertNext(e -> assertThat(e).isZero())
                .assertNext(e -> assertThat(e).isEqualTo(1))
                .assertNext(e -> assertThat(e).isEqualTo(2))
                .assertNext(e -> assertThat(e).isEqualTo(42))
                .assertNext(e -> assertThat(e).isEqualTo(3))
                .assertNext(e -> assertThat(e).isEqualTo(4))
                .assertNext(e -> assertThat(e).isEqualTo(5))
                .verifyComplete();
    }

    private record SwitchPack(
            Switch<?> mode,
            Switch<?> running,
            Switch<?> fan
            ) {}
}
