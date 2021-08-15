package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.HvacDeviceStatus;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.util.Set;

/**
 * A device with just one switch acting as an HVAC device that just supports one mode (either heating or cooling).
 *
 * Examples of this device are - a simple fan, a whole house fan, a heater fan, a radiant heater and so on.
 *
 * @see net.sf.dz3r.model.SingleStageUnitController
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SwitchableHvacDevice extends AbstractHvacDevice {

    private final HvacMode mode;
    private final Switch theSwitch;

    /**
     * Create a named instance.
     *
     * @param name Device name.
     * @param mode Supported mode. There can be only one.
     */
    public SwitchableHvacDevice(String name, HvacMode mode, Switch theSwitch) {
        super(name);

        this.mode = mode;
        this.theSwitch = theSwitch;

        check(theSwitch, "main");
    }

    @Override
    public Set<HvacMode> getModes() {
        return Set.of(mode);
    }

    @Override
    public Flux<Signal<HvacDeviceStatus, Void>> compute(Flux<Signal<HvacCommand, Void>> in) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "Switchable HVAC Device",
                getAddress(),
                "Turns on and off to provide " + mode.description.toLowerCase());
    }
}
