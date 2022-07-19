package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Single stage heatpump, energize to heat.
 *
 * Use the reversed {@link #switchMode} for "energize to cool" heat pumps.
 *
 * Initial mode is undefined and must be set by control logic, until that is done, any other commands are refused.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class HeatPump extends AbstractHvacDevice {

    /**
     * Default mode change delay.
     */
    private static final Duration DEFAULT_MODE_CHANGE_DELAY = Duration.ofSeconds(10);

    private final Switch<?> switchMode;
    private final Switch<?> switchRunning;
    private final Switch<?> switchFan;

    private final boolean reverseMode;
    private final boolean reverseRunning;
    private final boolean reverseFan;

    private final Duration modeChangeDelay;

    /**
     * Requested device state.
     *
     * All commands fed into {@link #compute(Flux)} will result in error signals until the operating {@link HvacMode} is set.
     */
    private HvacCommand requested = new HvacCommand(null, null, null);

    /**
     * Actual device state.
     */
    private HvacCommand actual = new HvacCommand(null, null, null);

    /**
     * Create an instance with all straight switches.
     *
     * @param name JMX name.
     * @param switchMode Switch to pull to change the operating mode.
     * @param switchRunning Switch to pull to turn on the compressor.
     * @param switchFan Switch to pull to turn on the air handler.
     */
    public HeatPump(String name, Switch<?> switchMode, Switch<?> switchRunning, Switch<?> switchFan) {
        this(name, switchMode, false, switchRunning, false, switchFan, false);
    }

    /**
     * Create an instance with some switches possibly reverse.
     *
     * @param name JMX name.
     * @param switchMode Switch to pull to change the operating mode.
     * @param reverseMode {@code true} if the "off" mode position corresponds to logical one.
     * @param switchRunning Switch to pull to turn on the compressor.
     * @param reverseRunning {@code true} if the "off" running position corresponds to logical one.
     * @param switchFan Switch to pull to turn on the air handler.
     * @param reverseFan {@code true} if the "off" fan position corresponds to logical one.
     */
    protected HeatPump(
            String name,
            Switch<?> switchMode, boolean reverseMode,
            Switch<?> switchRunning, boolean reverseRunning,
            Switch<?> switchFan, boolean reverseFan) {
        this(name,
                switchMode, reverseMode,
                switchRunning, reverseRunning,
                switchFan, reverseFan,
                DEFAULT_MODE_CHANGE_DELAY);
    }

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
     */
    protected HeatPump(
            String name,
            Switch<?> switchMode, boolean reverseMode,
            Switch<?> switchRunning, boolean reverseRunning,
            Switch<?> switchFan, boolean reverseFan,
            Duration changeModeDelay) {

        super(name);

        check(switchMode, "mode");
        check(switchRunning, "running");
        check(switchFan, "fan");

        this.switchMode = switchMode;
        this.switchRunning = switchRunning;
        this.switchFan = switchFan;

        this.reverseMode = reverseMode;
        this.reverseRunning = reverseRunning;
        this.reverseFan = reverseFan;

        this.modeChangeDelay = changeModeDelay;
    }

    @Override
    public Set<HvacMode> getModes() {
        return Set.of(HvacMode.COOLING, HvacMode.HEATING);
    }

    @Override
    public Flux<Signal<HvacDeviceStatus, Void>> compute(Flux<Signal<HvacCommand, Void>> in) {

        // Shut off the condenser, let the fan be as is
        Flux<Signal<HvacCommand, Void>> init = Flux.just(
                new Signal<>(clock.instant(), new HvacCommand(null, 0.0, null))
        );

        // Shut off everything
        Flux<Signal<HvacCommand, Void>> shutdown = Flux.just(
                new Signal<>(clock.instant(), new HvacCommand(null, 0.0, 0.0))
        );

        return Flux.concat(init, in, shutdown)
                .filter(Signal::isOK)
                .filter(ignored -> !isClosed())
                .flatMap(s -> Flux.create(sink -> process(s, sink)));
    }

    private void process(Signal<HvacCommand, Void> signal, FluxSink<Signal<HvacDeviceStatus, Void>> sink) {

        try {

            logger.debug("process: {}", signal);

            checkInitialMode(signal);
            trySetMode(signal, sink);
            setOthers(signal, sink);

        } catch (Throwable t) { // NOSONAR Consequences have been considered

            logger.error("Failed to compute {}", signal, t);
            sink.next(new Signal<>(clock.instant(), null, null, Signal.Status.FAILURE_TOTAL, t));

        } finally {
            sink.complete();
        }

    }

    private void checkInitialMode(Signal<HvacCommand, Void> signal) {

        if (requested.mode == null
                && signal.getValue().mode == null
                && signal.getValue().demand > 0) {

            throw new IllegalStateException("Can't accept demand > 0 before setting the operating mode");
        }
    }

    private void trySetMode(Signal<HvacCommand, Void> signal, FluxSink<Signal<HvacDeviceStatus, Void>> sink) throws IOException {

        var newMode = signal.getValue().mode;

        if (signal.getValue().mode == null) {
            return;
        }

        if (newMode == requested.mode) {
            logger.debug("Mode unchanged: {}", newMode);
            return;
        }

        logger.info("Mode changing to: {}", signal.getValue().mode);

        // Now careful, need to shut off the condenser (don't care about the fan) and wait to avoid damaging the hardware
        // ... but only if it is already running

        if (actual.demand != null && actual.demand > 0) {

            logger.info("Shutting off the condenser");

            var requestedDemand = reconcile(actual, new HvacCommand(null, 0.0, null));
            sink.next(
                    new Signal<>(clock.instant(),
                            new HeatpumpStatus(
                                    HvacDeviceStatus.Kind.REQUESTED,
                                    requestedDemand,
                                    actual,
                                    uptime())));

            setRunning(reverseRunning);
            updateUptime(clock.instant(), false);

            // Note, #requested is not set - this is a transition
            actual = reconcile(actual, requestedDemand);

            sink.next(
                    new Signal<>(clock.instant(),
                            new HeatpumpStatus(
                                    HvacDeviceStatus.Kind.ACTUAL,
                                    requestedDemand,
                                    actual,
                                    uptime())));
            logger.warn("Letting the hardware settle for modeChangeDelay={}", modeChangeDelay);
            Mono.delay(modeChangeDelay).block();

        } else {
            logger.debug("Condenser is not running, skipping the pause");
        }

        requested = reconcile(
                actual,
                new HvacCommand(newMode, null, null));
        sink.next(
                new Signal<>(clock.instant(),
                        new HeatpumpStatus(
                                HvacDeviceStatus.Kind.REQUESTED,
                                requested,
                                actual,
                                uptime())));
        setMode((newMode == HvacMode.HEATING) != reverseMode);
        actual = reconcile(actual, requested);
        sink.next(
                new Signal<>(clock.instant(),
                        new HeatpumpStatus(
                                HvacDeviceStatus.Kind.ACTUAL,
                                requested,
                                actual,
                                uptime())));
        logger.info("Mode changed to: {}", signal.getValue().mode);
    }

    /**
     * Reconcile the incoming command with the current state.
     *
     *
     * @param previous Previous command.
     * @param next Incoming command.
     *
     * @return Command that will actually be executed.
     *
     * @throws IllegalArgumentException if the command indicates an unsupported mode, or illegal fan state.
     */
    private HvacCommand reconcile(HvacCommand previous, HvacCommand next) {

        var result = new HvacCommand(
                next.mode == null? previous.mode : next.mode,
                next.demand == null ? previous.demand : next.demand,
                next.fanSpeed == null ? previous.fanSpeed : next.fanSpeed
        );

        logger.debug("Reconcile: {} + {} => {}", previous, next, result);

        return result;
    }

    /**
     * Set the condenser and fan switches to proper positions.
     *
     * Note that the fan switch is only set if {@link HvacCommand#fanSpeed} is not {@code null},
     * but {@link HvacCommand#demand} is expected to have a valida value.
     *
     * @param signal Signal to set the state according to.
     * @param sink Sink to report hardware status to.
     * @throws IOException if there was a problem talking to switch hardware.
     */
    private void setOthers(Signal<HvacCommand, Void> signal, FluxSink<Signal<HvacDeviceStatus, Void>> sink) throws IOException {

        var command = signal.getValue();
        var requestedOperation = reconcile(
                actual,
                new HvacCommand(null, command.demand, command.fanSpeed));
        sink.next(
                new Signal<>(clock.instant(),
                        new HeatpumpStatus(
                                HvacDeviceStatus.Kind.REQUESTED,
                                requestedOperation,
                                actual,
                                uptime())));

        setRunning((requestedOperation.demand > 0) != reverseRunning);
        updateUptime(clock.instant(), requestedOperation.demand > 0);

        if (requestedOperation.fanSpeed != null) {
            setFan((requestedOperation.fanSpeed > 0) != reverseFan);
            updateUptime(clock.instant(), requestedOperation.fanSpeed > 0);
        }
        actual = reconcile(actual, requestedOperation);

        sink.next(
                new Signal<>(clock.instant(),
                        new HeatpumpStatus(
                                HvacDeviceStatus.Kind.ACTUAL,
                                requestedOperation,
                                actual,
                                uptime())));
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Single Stage Heatpump Driver (energize to heat)",
                getAddress(),
                "Controls single stage heat pump");
    }

    @Override
    protected void doClose() throws IOException {

        logger.warn("Shutting down");

        switchRunning.setState(false).block();
        switchFan.setState(false).block();
        switchMode.setState(false).block();
        logger.info("Shut down.");
    }

    public static class HeatpumpStatus extends HvacDeviceStatus {

        public final HvacCommand actual;

        protected HeatpumpStatus(Kind kind, HvacCommand requested, HvacCommand actual, Duration uptime) {
            super(kind, requested, uptime);
            this.actual = actual;
        }

        @Override
        public String toString() {
            return "{kind=" + kind + ", requested=" + requested + ", actual=" + actual + ", uptime=" + uptime + "}";
        }
    }

    protected void setMode(boolean state) throws IOException { // NOSONAR Subclass throws this exception
        switchMode.setState(state).block();
    }

    protected void setRunning(boolean state) throws IOException { // NOSONAR Subclass throws this exception
        switchRunning.setState(state).block();
    }

    protected void setFan(boolean state) throws IOException { // NOSONAR Subclass throws this exception
        switchFan.setState(state).block();
    }
}
