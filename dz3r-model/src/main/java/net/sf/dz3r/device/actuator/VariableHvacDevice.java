package net.sf.dz3r.device.actuator;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.counter.ResourceUsageCounter;
import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static net.sf.dz3r.device.actuator.VariableOutputDevice.OutputState;

/**
 * A device with just one actuator acting as an HVAC device that just supports one mode (either heating or cooling).
 *
 * Very much like {@link SwitchableHvacDevice}, but supports {@link VariableOutputDevice variable output devices}.
 *
 * Examples of this device are - variable speed fan, whole house fan, heater fan, controllable radiant heater and so on.
 *
 * @see net.sf.dz3r.model.SingleStageUnitController
 * @see SwitchableHvacDevice
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class VariableHvacDevice extends SingleModeHvacDevice<OutputState> {

    private final VariableOutputDevice actuator;

    protected VariableHvacDevice(Clock clock, String name, HvacMode mode, VariableOutputDevice actuator, ResourceUsageCounter<Duration> uptimeCounter) {
        super(clock, name, mode, uptimeCounter);

        this.actuator = HCCObjects.requireNonNull(actuator, "actuator can't be null");
    }

    @Override
    protected Flux<Signal<HvacDeviceStatus<OutputState>, Void>> apply(HvacCommand command) {

        var output = Math.max(command.demand, command.fanSpeed);
        var on = Double.compare(output, 0d) != 0;

        return Flux.just(
                new Signal<>(
                        Instant.now(clock),
                        translate(command, actuator.setState(on, output))
                )
        );
    }

    private HvacDeviceStatus<OutputState> translate(HvacCommand command, DeviceState<OutputState> state) {
        return new HvacDeviceStatus<>(
                command,
                uptime(),
                state
        );
    }

    @Override
    protected void doClose() throws IOException {

    }
}
