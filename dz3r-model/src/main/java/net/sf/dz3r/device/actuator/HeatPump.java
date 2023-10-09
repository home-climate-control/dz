package net.sf.dz3r.device.actuator;

import net.sf.dz3r.counter.ResourceUsageCounter;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import org.apache.logging.log4j.LogManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static net.sf.dz3r.signal.Signal.Status.FAILURE_TOTAL;

/**
 * Single stage heatpump, energize to heat.
 *
 * Use the reversed {@link #switchMode} for "energize to cool" heat pumps.
 *
 * Initial mode is undefined and must be set by control logic; until that is done, any other commands are refused.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class HeatPump extends AbstractHvacDevice {

    /**
     * Default mode change delay.
     */
    private static final Duration DEFAULT_MODE_CHANGE_DELAY = Duration.ofSeconds(10);
    private static final Reconciler reconciler = new Reconciler();

    private final Switch<?> switchMode;
    private final Switch<?> switchRunning;

    /**
     * Fan hardware control switch.
     *
     * @see #switchFanStack
     */
    private final Switch<?> switchFan;

    /**
     * Fan logical control switch.
     *
     * @see #switchFan
     */
    private final StackingSwitch switchFanStack;

    private final boolean reverseMode;
    private final boolean reverseRunning;
    private final boolean reverseFan;
    private final Duration modeChangeDelay;

    private final Scheduler scheduler;

    /**
     * Requested device state.
     *
     * All commands fed into {@link #compute(Flux)} will result in error signals until the operating {@link HvacMode} is set.
     * All decisions about the device state are made based on this state only - the actual state would be much deferred
     * against the requested, and cannot be relied upon. Necessary changes are applied against this variable immediately
     * so that subsequent commands in the stream know what they are dealing with.
     */
    private HvacCommand requestedState = new HvacCommand(null, null, null);

    /**
     * Create an instance with some switches possibly reverse, and a given change mode delay.
     *
     * @param name JMX name.
     * @param switchMode Switch to pull to change the operating mode.
     * @param reverseMode {@code true} if the "off" mode position corresponds to logical one.
     * @param switchRunning Switch to pull to turn on the compressor.
     * @param reverseRunning {@code true} if the "off" running position corresponds to logical one.
     * @param switchFan Switch to pull to turn on the air handler.
     * @param reverseFan {@code true} if the "off" fan position corresponds to logical one.
     * @param changeModeDelay Delay to observe while changing the {@link HvacMode operating mode}.
     * @param uptimeCounter Self-explanatory. Optional for now.
     */
    public HeatPump(
            String name,
            Switch<?> switchMode, boolean reverseMode,
            Switch<?> switchRunning, boolean reverseRunning,
            Switch<?> switchFan, boolean reverseFan,
            Duration changeModeDelay,
            ResourceUsageCounter<Duration> uptimeCounter) {
        this(name,
                switchMode, reverseMode,
                switchRunning, reverseRunning,
                switchFan, reverseFan,
                changeModeDelay,
                uptimeCounter,
                Schedulers.newSingle("HeatPump(" + name + ")"));
    }
    public HeatPump(
            String name,
            Switch<?> switchMode, boolean reverseMode,
            Switch<?> switchRunning, boolean reverseRunning,
            Switch<?> switchFan, boolean reverseFan,
            Duration changeModeDelay,
            ResourceUsageCounter<Duration> uptimeCounter,
            Scheduler scheduler) {

        super(name, uptimeCounter);

        check(switchMode, "mode");
        check(switchRunning, "running");
        check(switchFan, "fan");

        this.switchMode = switchMode;
        this.switchRunning = switchRunning;
        this.switchFan = switchFan;

        // There are two virtual switches linked to this switch:
        //
        // "demand" - controlled by heating and cooling operations
        // "ventilation" - controlled by explicit requests to turn the fan on or off

        this.switchFanStack = new StackingSwitch("fan", switchFan);

        this.reverseMode = reverseMode;
        this.reverseRunning = reverseRunning;
        this.reverseFan = reverseFan;

        this.modeChangeDelay = Optional.ofNullable(changeModeDelay).orElseGet(() -> {
            logger.warn("using default mode change delay of {}", DEFAULT_MODE_CHANGE_DELAY);
            return DEFAULT_MODE_CHANGE_DELAY;
        });

        this.scheduler = scheduler;
    }

    @Override
    public Set<HvacMode> getModes() {
        return Set.of(HvacMode.COOLING, HvacMode.HEATING);
    }

    @Override
    public Flux<Signal<HvacDeviceStatus, Void>> compute(Flux<Signal<HvacCommand, Void>> in) {

        // Shut off the condenser, let the fan be as is
        var init = Flux.just(new HvacCommand(null, 0.0, null));

        var commands = in
                .filter(Signal::isOK)
                .map(Signal::getValue)
                // We will only ignore incoming commands, but not shutdown
                .filter(ignored -> !isClosed());

        // Shut off everything
        var shutdown = Flux.just(new HvacCommand(null, 0d, 0d));

        return Flux
                .concat(init, commands, shutdown)
                .publishOn(scheduler)
                .flatMap(this::process)
                .doOnNext(this::broadcast);
    }

    private Flux<Signal<HvacDeviceStatus, Void>> process(HvacCommand command) {

        logger.debug("{}: process: {}", getAddress(), command);

        // This is the only condition that gets checked before the requested state is updated -
        // this is an invalid update and must be discarded

        if (!isModeSet(command)) {
            return Flux.just(
                    new Signal<>(
                            clock.instant(),
                            null,
                            null,
                            FAILURE_TOTAL,
                            new IllegalStateException("Demand command issued before mode is set (likely programming error): " + command))
            );
        }

        var change = reconciler.reconcile(getAddress(), requestedState, command);

        // This is the only time we touch requested state, otherwise side effects will explode the command pipeline
        requestedState = change.command;

        Flux<Signal<HvacDeviceStatus, Void>> modeFlux = change.modeChangeRequired ? setMode(command.mode, change.delayRequired) : Flux.empty();
        var stateFlux = setState(command);

        return Flux.concat(modeFlux, stateFlux);
    }

    /**
     * Check if the initial mode set.
     *
     * @param command Incoming command.
     * @return {@code true} if the mode is set and we can proceed, {@code false} otherwise
     */
    private boolean isModeSet(HvacCommand command) {
        return requestedState.mode != null || command.mode != null || command.demand <= 0;
    }

    /**
     * Issue a command sequence to change the operating mode. No sanity checking is performed.
     *
     *
     * @param mode New mode to set
     * @param needDelay {@code true} if a delay before setting the new mode is required.
     *
     * @return Flux of commands to change the operating mode.
     */
    private Flux<Signal<HvacDeviceStatus, Void>> setMode(HvacMode mode, boolean needDelay) {

        // May or may not be empty, see comments inside
        Flux<Signal<HvacDeviceStatus, Void>>  condenserOff = needDelay
                ? stopCondenser().doOnSubscribe(ignore -> logger.info("{}: mode changing to: {}", getAddress(), mode))
                : Flux.empty();
        var forceMode = forceMode(mode);

        return Flux
                .concat(condenserOff, forceMode)
                .doOnNext(s -> logger.debug("{}: setMode: {}", getAddress(), s.getValue().command))
                .doOnComplete(() -> logger.info("{}: mode changed to: {}", getAddress(), mode));
    }

    /**
     * Stop the condenser, then sleep for {@link #modeChangeDelay}.
     */
    private Flux<Signal<HvacDeviceStatus, Void>> stopCondenser() {

        return Flux
                .just(new StateCommand(switchRunning, reverseRunning))
                .doOnNext(ignore -> logger.info("{}: stopping the condenser", getAddress()))
                .flatMap(this::setState)
                .doOnNext(ignore -> logger.warn("{}: letting the hardware settle for modeChangeDelay={}", getAddress(), modeChangeDelay))

                // VT: FIXME: This doesn't work where as it should (see test cases) and allows the next main sequence element to jump ahead, why?
//                .delayElements(modeChangeDelay, scheduler)
//                .publishOn(scheduler)

                .flatMap(ignore -> Mono.create(sink -> {
                    // VT: NOTE: Calling delayElement() of Flux or Mono breaks things, need to figure out why
                    try {
                        // VT: FIXME: Need to find a lasting solution for this
                        // For now, this should be fine as long as the output from this flux is used in a sane way.
                        logger.warn("{}: BLOCKING WAIT FOR {}", getAddress(), modeChangeDelay);
                        Thread.sleep(modeChangeDelay.toMillis());
                        logger.warn("{}: blocking wait for {} DONE", getAddress(), modeChangeDelay);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        logger.warn("interrupted, nothing we can do about it", ex);
                    } finally {
                        // Ah, screw it
                        sink.success(true);
                    }
                }))
                .map(ignore ->
                        // If we're here, this means that the operation was carried out successfully
                        new Signal<>(clock.instant(),
                                new HvacDeviceStatus(
                                        // Informational only, but still verifiable
                                        reconciler.reconcile(
                                                getAddress(),
                                                requestedState,
                                                new HvacCommand(null, 0.0, null)).command,
                                        uptime()))
                );
    }

    /**
     * Set the mode, unconditionally. It is expected that all precautions have already been taken.
     *
     * @param mode Mode to set.
     * @return The flux of commands to set the mode.
     */
    private Flux<Signal<HvacDeviceStatus, Void>> forceMode(HvacMode mode) {

        return Flux
                .just(new StateCommand(switchMode, (mode == HvacMode.HEATING) != reverseMode))
                .doOnNext(command -> logger.debug("{}: setting mode={}", getAddress(), command))
                .flatMap(this::setState)
                .map(ignore ->
                        // If we're here, this means that the operation was carried out successfully
                        new Signal<>(clock.instant(),
                                new HvacDeviceStatus(
                                        reconciler.reconcile(
                                                getAddress(),
                                                requestedState,
                                                new HvacCommand(mode, null, null))
                                                .command,
                                        uptime()))
                );
    }

    private Mono<Boolean> setState(StateCommand command) {
        logger.debug("{}: setState({})={}", getAddress(), command.target, command.state);
        return command.target.setState(command.state);
    }

    /**
     * Set the condenser and fan switches to proper positions.
     *
     * Note that the fan switch is only set if {@link HvacCommand#fanSpeed} is not {@code null},
     * but {@link HvacCommand#demand} is expected to have a valid value.
     *
     * @param command Command to execute.
     */
    private Flux<Signal<HvacDeviceStatus, Void>> setState(HvacCommand command) {

        var requestedOperation = reconciler.reconcile(
                getAddress(),
                requestedState,
                new HvacCommand(null, command.demand, command.fanSpeed))
                .command;

        Flux<Boolean> runningFlux;
        if (requestedOperation.demand != null) {
            var running = (requestedOperation.demand > 0) != reverseRunning;
            runningFlux = Flux
                    .just(new StateCommand(switchRunning, running))
                    .flatMap(this::setState)
                    .doOnComplete(() -> updateUptime(clock.instant(), requestedOperation.demand > 0));
        } else {
            // This will cause no action, but will prompt zip() to do what it is expected to
            runningFlux = Flux.just(false);
        }

        Flux<Boolean> fanFlux;

        if (requestedOperation.fanSpeed != null) {
            var fan =(requestedOperation.fanSpeed > 0) != reverseFan;
            fanFlux = Flux
                    .just(new StateCommand(switchFan, fan))
                    .flatMap(this::setState)
                    .doOnComplete(() -> updateUptime(clock.instant(), requestedOperation.fanSpeed > 0));
        } else {
            // This will cause no action, but will prompt zip() to do what it is expected to
            fanFlux = Flux.just(false);
        }

        return Flux
                .zip(runningFlux, fanFlux)
                .doOnNext(z -> logger.debug("{}: zip(running, fan) received: ({}, {})", getAddress(), z.getT1(), z.getT2()))
                .map(pair ->
                    // If we're here, this means that the operation was carried out successfully
                    new Signal<>(clock.instant(),
                            new HvacDeviceStatus(
                                    requestedOperation,
                                    uptime()))
                );
    }

    @Override
    protected void doClose() throws IOException {

        logger.warn("Shutting down: {}", getAddress());

        Flux.just(
                        switchRunning,
                        switchFan,
                        switchMode)
                .flatMap(s -> s.setState(false))
                .blockLast();

        switchRunning.setState(false).block();
        switchFan.setState(false).block();
        switchMode.setState(false).block();

        logger.info("Shut down: {}", getAddress());
    }

    @Deprecated
    protected Mono<Boolean> setMode(boolean state) {
        return switchMode.setState(state);
    }

    @Deprecated
    protected Mono<Boolean> setRunning(boolean state) {
        return switchRunning.setState(state);
    }

    @Deprecated
    protected Mono<Boolean> setFan(boolean state) {
        return switchFanStack.getSwitch("demand").setState(state);
    }

    static class Reconciler {

        record Result(
                HvacCommand command,
                boolean modeChangeRequired,
                boolean delayRequired
        ) {}

        /**
         * Reconcile the incoming command with the current state.
         *
         * It is expected that the result will always take place of the {@code previous} argument.
         *
         * @param name Heat pump name.
         * @param previous Previous command.
         * @param next Incoming command.
         *
         * @return Command that will actually be executed, along with mode change flags.
         *
         * @throws IllegalArgumentException if the command indicates an unsupported mode, or illegal fan state.
         */
        public Result reconcile(String name, HvacCommand previous, HvacCommand next) {

            var result = new HvacCommand(
                    next.mode == null? previous.mode : next.mode,
                    next.demand == null ? previous.demand : next.demand,
                    next.fanSpeed == null ? previous.fanSpeed : next.fanSpeed
            );

            var modeChangeRequired = previous.mode != result.mode;
            var delayRequired = previous.mode != null && previous.mode != result.mode;

            LogManager.getLogger(HeatPump.class).debug("{}: reconcile: {} + {} => {}", name, previous, next, result);

            // Once set, mode will never go null again if the calling conventions are honored

            if (result.mode == null && result.demand != null && result.demand > 0) {
                throw new IllegalArgumentException("positive demand with no mode, programming error: " + result);
            }

            return new Result(result, modeChangeRequired, delayRequired);
        }
    }

    private record StateCommand(
            Switch<?> target,
            boolean state
    ) {}
}
