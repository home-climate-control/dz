package net.sf.dz3r.device.actuator;

import net.sf.dz3r.jmx.JmxDescriptor;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A device with just one switch acting as an HVAC device that just supports one mode (either heating or cooling).
 *
 * Examples of this device are - a simple fan, a whole house fan, a heater fan, a radiant heater and so on.
 *
 * For safety, it is assumed that cooling-only devices will support ventilation requests, while heating-only devices don't.
 * Exceptions such as a furnace with a fan for now will need to be handled with the {@link HeatPump} driver with
 * {@link NullSwitch} provided for {@link HeatPump#setMode(boolean) mode switch}.
 *
 * @see net.sf.dz3r.model.SingleStageUnitController
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SwitchableHvacDevice extends AbstractHvacDevice {

    private final HvacMode mode;
    private final Switch<?> theSwitch;
    private final boolean inverted;

    /**
     * Requested device state.
     *
     * {@code null} value means the status is unknown.
     *
     * Note that for cooling-only devices the {@link #actual} state is determined by whether
     * {@link HvacCommand#demand} OR {@link HvacCommand#fanSpeed} are positive.
     */
    private HvacCommand requested;

    /**
     * Create a named instance.
     *
     * @param name Device name.
     * @param mode Supported mode. There can be only one.
     */
    public SwitchableHvacDevice(String name, HvacMode mode, Switch<?> theSwitch) {
        this(name, mode, theSwitch, false);
    }

    /**
     * Create a named instance, possibly inverted.
     *
     * @param name Device name.
     * @param mode Supported mode. There can be only one.
     * @param inverted {@code true} if the switch is inverted.
     */
    public SwitchableHvacDevice(String name, HvacMode mode, Switch<?> theSwitch, boolean inverted) {
        super(name);

        this.mode = mode;
        this.inverted = inverted;

        this.requested = new HvacCommand(mode, null, null);

        this.theSwitch = theSwitch;
        check(theSwitch, "main");
    }

    @Override
    public Set<HvacMode> getModes() {
        return Set.of(mode);
    }

    @Override
    public Flux<Signal<HvacDeviceStatus, Void>> compute(Flux<Signal<HvacCommand, Void>> in) {
        return computeNonBlocking(in);
    }

    private Flux<Signal<HvacDeviceStatus, Void>> computeNonBlocking(Flux<Signal<HvacCommand, Void>> in) {

        return in
                .filter(Signal::isOK)

                // Can't throw this as a payload like we did in a blocking version, need to complain
                .doOnNext(signal -> logger.debug("compute signal={}", signal))

                .map(Signal::getValue)
                .map(this::reconcile)
                .filter(Predicate.not(this::isModeOnly))
                .doOnNext(command -> logger.debug("compute command={}", command))
                .flatMap(command -> {

                    var state = getState(command);

                    logger.debug("state: {}", state);

                    // By this time, the command has been verified to be valid
                    requested = command;

                    var result = new HvacDeviceStatus(command, uptime());

                    return theSwitch
                            .setState(state != inverted)
                            .map(ignore -> {
                                updateUptime(clock.instant(), state);
                                return new Signal<>(clock.instant(), result);
                            });
                });
    }

    /**
     * Retanied for analysis. Should be removed as soon as normal operation is confirmed.
     *
     * @deprecated
     */
    @Deprecated(forRemoval = true, since = "2023-10-01")
    Flux<Signal<HvacDeviceStatus, Void>> computeBlocking(Flux<Signal<HvacCommand, Void>> in) {
        return in
                .filter(Signal::isOK)
                .flatMap(signal -> {
                    return Flux
                            .<Signal<HvacDeviceStatus, Void>>create(sink -> {

                                try {

                                    var command = reconcile(signal.getValue());

                                    if (isModeOnly(command)) {
                                        return;
                                    }

                                    var state = getState(command);

                                    logger.debug("State: {}", state);

                                    sink.next(new Signal<>(clock.instant(), new HvacDeviceStatus(command, uptime())));

                                    // By this time, the command has been verified to be valid
                                    requested = command;

                                    theSwitch.setState(state != inverted).block();

                                    // No longer relevant
                                    //actual = state;

                                    updateUptime(clock.instant(), state);

                                    var complete = new HvacDeviceStatus(command, uptime());
                                    sink.next(new Signal<>(clock.instant(), complete));

                                } catch (Throwable t) { // NOSONAR Consequences have been considered

                                    logger.error("Failed to compute {}", signal, t);
                                    sink.next(new Signal<>(clock.instant(), null, null, Signal.Status.FAILURE_TOTAL, t));

                                } finally {
                                    sink.complete();
                                }

                            });
                })
                .doOnNext(this::broadcast);
    }

    private boolean isModeOnly(HvacCommand command) {

        // A valid situation for the whole system which makes no sense for this particular application

        if (command.demand == null && command.fanSpeed == null) {
            logger.warn("mode only command, ignored: {}", command);
            return true;
        }

        return false;
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
    private HvacCommand reconcile(HvacCommand command) {

        if (command.mode != null && command.mode != mode) {
            throw new IllegalArgumentException(command.mode.description + " is not supported by this instance");
        }

        if (mode != HvacMode.COOLING && command.fanSpeed != null && command.fanSpeed > 0) {
            // FIXME: https://github.com/home-climate-control/dz/issues/222
            logger.warn("fanSpeed>0 should not be issued to this device in heating mode, ignored. Kick the maintainer to fix #222 (command={})", command);
        }

        var result = new HvacCommand(
                mode,
                command.demand == null ? requested.demand : command.demand,
                command.fanSpeed == null ? requested.fanSpeed : command.fanSpeed
        );

        logger.debug("Requested: {}", result);

        return result;
    }

    /**
     * Extract the switch state out of the command.
     *
     * @param command Command to execute.
     *
     * @return Switch state.
     */
    private boolean getState(HvacCommand command) {
        return Optional.ofNullable(command.demand).orElse(0d) + Optional.ofNullable(command.fanSpeed).orElse(0d) > 0;
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "Switchable HVAC Device",
                getAddress(),
                "Turns on and off to provide " + mode.description.toLowerCase());
    }

    @Override
    protected void doClose() {
        logger.warn("Shutting down: {}", getAddress());
        logger.warn("close(): setting {} to off", theSwitch.getAddress());
        theSwitch.setState(inverted).block();
        logger.info("Shut down: {}", getAddress());
    }
}
