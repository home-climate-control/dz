package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.hcc.device.DeviceState;
import com.homeclimatecontrol.hcc.model.HvacMode;
import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.HvacCommand;
import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.counter.ResourceUsageCounter;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

import static net.sf.dz3r.device.actuator.VariableOutputDevice.Command;
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

    /**
     * Split the output value into this many bands.
     *
     * @see #band(double, int)
     */
    private int bandCount;

    /**
     * Set the output power to at most this percentage of the total.
     *
     * This value is further split into {@link #setBandCount(int) bands}. Valid values are from 0 to 1.0
     * (though why would you want to set it to 0?)
     */
    private double maxPower;

    public VariableHvacDevice(
            Clock clock,
            String name,
            HvacMode mode,
            VariableOutputDevice actuator,
            double maxPower,
            int bandCount,
            ResourceUsageCounter<Duration> uptimeCounter
    ) {
        super(clock, name, mode, uptimeCounter);

        this.actuator = HCCObjects.requireNonNull(actuator, "actuator can't be null");

        setBandCount(bandCount);
        setMaxPower(maxPower);

        logger.debug("{}: created with maxPower={}, bandCount={}", name, maxPower, bandCount);
    }

    public void setBandCount(int bandCount) {

        if (bandCount < 0) {
            throw new IllegalArgumentException("bandCount must be non-negative");
        }

        if (bandCount > 100) {
            throw new IllegalArgumentException("unreasonably high bandCount of " + bandCount + ", max=100");
        }

        this.bandCount = bandCount;
    }

    public int getBandCount() {
        return bandCount;
    }

    public void setMaxPower(double maxPower) {

        if (maxPower < 0.0) {
            throw new IllegalArgumentException("maxPower must be non-negative");
        }

        if (maxPower > 1.0) {
            throw new IllegalArgumentException("maxPower given (" + maxPower + " is outside of 0..1 range");
        }

        this.maxPower = maxPower;
    }

    public double getMaxPower() {
        return maxPower;
    }

    @Override
    protected Flux<Signal<HvacDeviceStatus<OutputState>, Void>> apply(HvacCommand command) {

        var output = band(Math.max(command.demand(), command.fanSpeed()), bandCount);
        var scaled = output * maxPower;
        var on = Double.compare(output, 0d) != 0;

        // VT: FIXME: Need to modify the hierarchy to include band count and scale into the output signal instead

        logger.trace("{}: on={}, output={}, bands={}, scaled={}", getAddress(), on, output, bandCount, scaled);

        return Flux.just(
                new Signal<>(
                        clock.instant(),
                        translate(command, actuator.setState(new Command(on, scaled)))
                )
        );
    }

    static double band(double source, int bandCount) {

        if (source < 0 || source > 1.0) {
            throw new IllegalArgumentException("source (" + source + ") is outside of 0..1 range");
        }

        // Zero means don't split at all
        if (bandCount == 0) {
            return source;
        }

        // Vale of zero translates into zero, value of 1.0 translates into 1.0.

        var expanded = source * bandCount;
        var ceiling = Math.ceil(expanded);
        return ceiling / bandCount;
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
