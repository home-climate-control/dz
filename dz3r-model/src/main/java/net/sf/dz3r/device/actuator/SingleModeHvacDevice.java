package net.sf.dz3r.device.actuator;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.counter.ResourceUsageCounter;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A HVAC device that just supports one mode (either heating or cooling).
 *
 * For safety, it is assumed that cooling-only devices will support ventilation requests, while heating-only devices don't.
 * Exceptions such as a furnace with a fan for now will need to be handled with the {@link HeatPump} driver with
 * {@link NullSwitch} provided for {@link HeatPump#setMode(boolean) mode switch}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class SingleModeHvacDevice<T> extends AbstractHvacDevice<T> {

    private static final HvacCommand OFF = new HvacCommand(null, 0.0, 0d);

    protected final HvacMode mode;

    /**
     * Requested device state.
     *
     * {@code null} value means the status is unknown.
     *
     * Note that for cooling-only devices the actual state is determined by whether
     * {@link HvacCommand#demand} OR {@link HvacCommand#fanSpeed} are positive.
     */
    protected HvacCommand requested;

    protected SingleModeHvacDevice(Clock clock, String name, HvacMode mode, ResourceUsageCounter<Duration> uptimeCounter) {
        super(clock, name, uptimeCounter);

        this.mode = HCCObjects.requireNonNull(mode, "mode can't be null");
        this.requested = new HvacCommand(mode, null, null);
    }

    @Override
    public Flux<Signal<HvacDeviceStatus<T>, Void>> compute(Flux<Signal<HvacCommand, Void>> in) {

        var init = Flux.just(OFF);
        var shutdown = Flux.just(OFF);

        var commands = in
                .doOnNext(signal -> logger.debug("{}: compute signal={}", getAddress(), signal))
                .map(this::parseInput)
                .filter(Predicate.not(this::isModeOnly))
                .doOnNext(command -> logger.debug("{}: compute   command={}", getAddress(), command))
                .map(this::reconcile)
                .doOnNext(command -> logger.debug("{}: compute reconciled={}", getAddress(), command))
                // We will only ignore incoming commands, but not shutdown
                .filter(ignored -> !isClosed());

        return Flux
                .concat(init, commands, shutdown)
                .flatMap(this::apply)
                .doOnNext(this::updateUptime)
                .doOnNext(this::broadcast);
    }

    /**
     * Pass non-error signal commands through, interpret errors as {@link #OFF} command.
     *
     * @param signal Incoming signal.
     * @return Command to execute.
     */
    private HvacCommand parseInput(Signal<HvacCommand, Void> signal) {

        if (!signal.isError()) {
            return signal.getValue();
        }

        logger.error("{}: error signal, sending OFF: {}", getAddress(), signal);

        return OFF;
    }

    /**
     * Reconcile the incoming command with the current state.
     *
     * @param command Incoming command.
     *
     * @return Command that will actually be executed.
     *
     * @throws IllegalArgumentException if the command indicates an unsupported mode, or illegal fan state.
     */
    protected HvacCommand reconcile(HvacCommand command) {

        if (command.mode != null && command.mode != mode) {
            throw new IllegalArgumentException(command.mode.description + " is not supported by this instance");
        }

        if (mode != HvacMode.COOLING && command.fanSpeed != null && command.fanSpeed > 0) {
            // FIXME: https://github.com/home-climate-control/dz/issues/222
            logger.warn("{}: fanSpeed>0 should not be issued to this device in heating mode, ignored. Kick the maintainer to fix #222 (command={})", getAddress(), command);
        }

        var result = new HvacCommand(
                mode,
                command.demand == null ? requested.demand : command.demand,
                command.fanSpeed == null ? requested.fanSpeed : command.fanSpeed
        );

        logger.debug("{}: requested: {}", getAddress(), result);

        return result;
    }

    /**
     * Send the command to the device, emit device status flux.
     *
     * @param command Command to execute.
     * @return Device status flux.
     */
    protected abstract Flux<Signal<HvacDeviceStatus<T>, Void>> apply(HvacCommand command);

    private final void updateUptime(Signal<HvacDeviceStatus<T>, Void> signal) {
        updateUptime(clock.instant(), signal.getValue().command.demand > 0);
    }

    @Override
    public final Set<HvacMode> getModes() {
        return Set.of(mode);
    }

    protected final boolean isModeOnly(HvacCommand command) {

        // A valid situation for the whole system which makes no sense for this particular application

        if (command.demand == null && command.fanSpeed == null) {
            logger.warn("{}: mode only command, ignored: {}", getAddress(), command);
            return true;
        }

        return false;
    }
}
