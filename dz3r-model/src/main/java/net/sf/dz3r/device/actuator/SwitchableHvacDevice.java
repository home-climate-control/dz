package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * A device with just one switch acting as an HVAC device that just supports one mode (either heating or cooling).
 *
 * Examples of this device are - a simple fan, a whole house fan, a heater fan, a radiant heater and so on.
 *
 * For simplicity, it is assumed that cooling-only devices provide ventilation, while heating-only devices don't.
 * Exceptions such as a furnace with a fan for now will need to be handled with the {@link HeatPump} driver.
 *
 * @see net.sf.dz3r.model.SingleStageUnitController
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SwitchableHvacDevice extends AbstractHvacDevice {

    private final HvacMode mode;
    private final Switch theSwitch;

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
     * Actual switch status.
     *
     * {@code null} value means the status is unknown.
     *
     * VT: FIXME: Associate a timestamp with this value.
     */
    private Boolean actual;

    /**
     * Create a named instance.
     *
     * @param name Device name.
     * @param mode Supported mode. There can be only one.
     */
    public SwitchableHvacDevice(String name, HvacMode mode, Switch theSwitch) {
        super(name);

        this.mode = mode;
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

        return in
                .filter(Signal::isOK)
                .flatMap(signal -> {
                    return Flux
                            .create(sink -> {

                                try {

                                    var command = reconcile(signal.getValue());

                                    if (isModeOnly(command)) {
                                        return;
                                    }

                                    var state = getState(command);

                                    logger.debug("State: {}", state);

                                    sink.next(new Signal<>(Instant.now(), new SwitchStatus(SwitchStatus.Kind.REQUESTED, command, actual, uptime())));

                                    // By this time, the command has been verified to be valid
                                    requested = command;

                                    theSwitch.setState(state);
                                    actual = state;
                                    updateUptime(state);

                                    var complete = new SwitchStatus(SwitchStatus.Kind.ACTUAL, command, actual, uptime());
                                    sink.next(new Signal<>(Instant.now(), complete));

                                } catch (Throwable t) { // NOSONAR Consequences have been considered

                                    logger.error("Failed to compute {}", signal, t);
                                    sink.next(new Signal<>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, t));

                                } finally {
                                    sink.complete();
                                }

                            });
                });
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

        if (mode != HvacMode.COOLING && command.fanSpeed > 0) {
            throw new IllegalArgumentException("fanSpeed=" + command.fanSpeed + " is not supported by this instance (not in cooling mode)");
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

    public static class SwitchStatus extends HvacDeviceStatus {

        public final Boolean actual;

        protected SwitchStatus(Kind kind, HvacCommand requested, Boolean actual, Duration uptime) {
            super(kind, requested, uptime);
            this.actual = actual;
        }

        @Override
        public String toString() {
            return "{kind=" + kind + ", requested=" + requested + ", actual=" + actual + ", uptime=" + uptime + "}";
        }
    }
}
