package net.sf.dz3r.model;

import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.ZoneStatus;
import reactor.core.publisher.Flux;

/**
 * Home climate control zone.
 *
 * A {@link Thermostat} is just a device that watches the temperature.
 * A zone is an entity that controls the thermostat.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class Zone implements ProcessController<Double, ZoneStatus, Double>, Addressable<String>, JmxAware {

    private final Thermostat ts;
    private ZoneSettings settings;

    public Zone(Thermostat ts) {
        this.ts = ts;
        settings = new ZoneSettings(ts.getSetpoint());
    }

    @Override
    public void setSetpoint(double setpoint) {
        ts.setSetpoint(setpoint);
    }

    @Override
    public double getSetpoint() {
        return ts.getSetpoint();
    }

    @Override
    public Signal<Double, Double> getProcessVariable() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public double getError() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public Flux<Signal<Status<ZoneStatus>, Double>> compute(Flux<Signal<Double, Double>> pv) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Get the human readable thermostat name.
     *
     * @return Thermostat name.
     */
    @Override
    public String getAddress() {
        return ts.getAddress();
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "Home Climate Control Zone",
                getAddress(),
                "Controls zone settings, collects sensor samples, and passes them to Zone Controller");
    }
}
