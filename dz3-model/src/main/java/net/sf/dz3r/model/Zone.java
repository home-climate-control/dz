package net.sf.dz3r.model;

import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

/**
 * Home climate control zone.
 *
 * A {@link Thermostat} is just a device that watches the temperature.
 * A zone is an entity that controls the thermostat.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class Zone implements ProcessController<Double, ZoneStatus, Void>, Addressable<String>, JmxAware {

    private final Logger logger = LogManager.getLogger();

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
    public Signal<Double, Void> getProcessVariable() {
        // VT: FIXME: This result depends on zone settings. Does it make sense at all?
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public double getError() {
        // VT: FIXME: This result depends on zone settings. Does it make sense at all?
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public Flux<Signal<Status<ZoneStatus>, Void>> compute(Flux<Signal<Double, Void>> pv) {

        // Since the zone doesn't need the payload, but the thermostat does, need to translate the input
        var stage0 = pv
                .map(s -> new Signal<>(s.timestamp, s.getValue(), (ProcessController.Status<Double>) null, s.status, s.error));

        // Not to disrupt the thermostat control logic, input signal is fed to it
        // regardless of whether the zone is enabled
        var stage1 = ts
                .compute(stage0)
                .doOnNext(e -> logger.trace("ts/{}: {}", getAddress(), e));

        // Now, dampen the signal if the zone is disabled
        var stage2 = stage1
                .map(this::suppressIfNotEnabled)
                .doOnNext(e -> logger.debug("isOn/{}: {} {}", getAddress(), settings.enabled ? "enabled" : "DISABLED", e));

        // Now, translate the signal into what we need on the way out
        return stage2
                .map(this::translate)
                .doOnNext(e -> logger.debug("final/{}: {}", getAddress(), e));
    }

    private Signal<Status<ZoneStatus>, Void> translate(Signal<Status<Double>, Status<Double>> source) {

        return new Signal<>(
                source.timestamp,
                createStatus(settings, source.getValue(), source.payload),
                null,
                source.status,
                source.error
                );
    }

    private Status<ZoneStatus> createStatus(ZoneSettings settings, Status<Double> status, Status<Double> payload) {
        // VT: FIXME: This method is bogus, need to properly implement it once I get some sleep
        return new Status<>(settings.setpoint, status.error, new ZoneStatus(settings, status, payload));
    }

    private Signal<Status<Double>, Status<Double>> suppressIfNotEnabled(Signal<Status<Double>, Status<Double>> source) {

        if (settings.enabled) {
            return source;
        }

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
