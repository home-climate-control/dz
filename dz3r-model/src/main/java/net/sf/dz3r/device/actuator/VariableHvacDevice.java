package net.sf.dz3r.device.actuator;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.counter.ResourceUsageCounter;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

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
public class VariableHvacDevice extends SingleModeHvacDevice {

    private final VariableOutputDevice actuator;

    protected VariableHvacDevice(Clock clock, String name, HvacMode mode, VariableOutputDevice actuator, ResourceUsageCounter<Duration> uptimeCounter) {
        super(clock, name, mode, uptimeCounter);

        this.actuator = HCCObjects.requireNonNull(actuator, "actuator can't be null");
    }


    @Override
    protected Flux<Signal<HvacDeviceStatus, Void>> apply(HvacCommand hvacCommand) {
        return Flux.empty();
    }

    @Override
    protected void doClose() throws IOException {

    }
}
