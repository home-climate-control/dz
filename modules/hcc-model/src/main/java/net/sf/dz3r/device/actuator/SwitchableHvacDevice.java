package net.sf.dz3r.device.actuator;

import net.sf.dz3r.counter.ResourceUsageCounter;
import net.sf.dz3r.model.HvacMode;
import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A device with just one switch acting as an HVAC device.
 *
 * Very much like {@link VariableHvacDevice}, but just supports on/off operation.
 *
 * Examples of this device are - a simple fan, a whole house fan, a heater fan, a radiant heater and so on.
 *
 * @see net.sf.dz3r.model.SingleStageUnitController
 * @see VariableHvacDevice
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class SwitchableHvacDevice extends SingleModeHvacDevice<Void> {

    private final CqrsSwitch<?> theSwitch;
    private final boolean inverted;


    /**
     * Create a named instance, possibly inverted.
     *
     * @param name Device name.
     * @param mode Supported mode. There can be only one.
     * @param inverted {@code true} if the switch is inverted.
     * @param uptimeCounter Self-explanatory. Optional for now.
     */
    public SwitchableHvacDevice(
            Clock clock,
            String name,
            HvacMode mode,
            CqrsSwitch<?> theSwitch,
            boolean inverted,
            ResourceUsageCounter<Duration> uptimeCounter
    ) {
        super(clock, name, mode, uptimeCounter);

        this.theSwitch = theSwitch;
        this.inverted = inverted;

        check(theSwitch, "main");
    }

    @Override
    public Flux<Signal<HvacDeviceStatus<Void>, Void>> compute(Flux<Signal<HvacCommand, Void>> in) {
        return
                computeNonBlocking(in)
                        .doOnNext(this::broadcast);
    }

    private Flux<Signal<HvacDeviceStatus<Void>, Void>> computeNonBlocking(Flux<Signal<HvacCommand, Void>> in) {

        return in
                .filter(Signal::isOK)

                // Can't throw this as a payload like we did in a blocking version, need to complain
                .doOnNext(signal -> logger.debug("{}: compute signal={}", getAddress(), signal))

                .map(Signal::getValue)
                .map(this::reconcile)
                .filter(Predicate.not(this::isModeOnly))
                .doOnNext(command -> logger.debug("{}: compute command={}", getAddress(), command))
                .flatMap(command -> {

                    var state = getState(command);

                    logger.debug("{}: state: {}{}", getAddress(), state != inverted, inverted ? " (inverted)" : "");

                    // By this time, the command has been verified to be valid
                    requested = command;

                    var result = new HvacDeviceStatus<Void>(command, uptime(), null);

                    theSwitch.setState(state != inverted);
                    updateUptime(clock.instant(), state);
                    return Flux.just(new Signal<>(clock.instant(), result));
                });
    }

    @Override
    protected Flux<Signal<HvacDeviceStatus<Void>, Void>> apply(HvacCommand command) {
        throw new IllegalStateException("refactoring incomplete");
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
    protected void doClose() {
        logger.warn("Shutting down: {}", getAddress());
        logger.warn("close(): setting {} to off", theSwitch.getAddress());
        theSwitch.setState(inverted);
        logger.info("Shut down: {}", getAddress());
    }
}
