package net.sf.dz3r.model;

import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.ThermostatStatus;
import net.sf.dz3r.signal.hvac.ZoneStatus;
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
public class Zone implements SignalProcessor<Double, ZoneStatus, String>, Addressable<String>, JmxAware {

    public enum State {

        ERROR,
        OFF,
        CALLING,
        HAPPY
    }

    private final Logger logger = LogManager.getLogger();

    private final Thermostat ts;

    /**
     * Zone settings.
     */
    private ZoneSettings settings;

    /**
     * Create an instance.
     *
     * @param ts Thermostat to use.
     * @param settings Zone settings. Thermostat setpoint overrides zone setpoint - this argument is here to configure initial flags.
     */
    public Zone(Thermostat ts, ZoneSettings settings) {
        this.ts = ts;
        setSettings(new ZoneSettings(settings, ts.getSetpoint()));
    }

    public void setSettings(ZoneSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("settings can't be null");
        }

        ts.setSetpoint(settings.setpoint);

        this.settings = this.settings == null ? settings : this.settings.merge(settings);

        logger.info("setSettings({}): {}", getAddress(), settings);
    }

    public ZoneSettings getSettings() {
        return settings;
    }

    @Override
    public Flux<Signal<ZoneStatus, String>> compute(Flux<Signal<Double, String>> in) {

        logger.debug("compute()");

        // Since the zone doesn't need the payload, but the thermostat does, need to translate the input
        var stage0 = in
                .map(s -> new Signal<>(s.timestamp, s.getValue(), (Void) null, s.status, s.error));

        // Not to disrupt the thermostat control logic, input signal is fed to it
        // regardless of whether the zone is enabled
        var stage1 = ts
                .compute(stage0)
                .doOnNext(e -> logger.trace("ts/{}: {}", getAddress(), e));

        // Now, need to translate into a form that is easier manipulated
        var stage2 = stage1.map(this::translate)
                .doOnNext(e -> logger.debug("translated/{}: {}", getAddress(), e));

        // Now, dampen the signal if the zone is disabled
        return stage2
                .map(this::suppressIfNotEnabled)
                .doOnNext(e -> logger.debug("isOn/{}: {} {}", getAddress(), settings.enabled ? "enabled" : "DISABLED", e));
    }

    private Signal<ZoneStatus, String> translate(Signal<ProcessController.Status<ThermostatStatus>, Void> source) {

        return new Signal<>(
                source.timestamp,
                new ZoneStatus(settings, source.getValue().signal),
                getAddress(),
                source.status,
                source.error);
    }

    private Signal<ZoneStatus, String> suppressIfNotEnabled(Signal<ZoneStatus, String> source) {

        if (settings.enabled) {
            return source;
        }

        return new Signal<>(
                source.timestamp,
                new ZoneStatus(new ZoneSettings(source.getValue().settings, false), new ThermostatStatus(0, false)),
                source.payload,
                source.status,
                source.error);
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

    @Override
    public String toString() {
        return "{zone name=" + ts.getAddress() + ", settings={" + settings + "}}";
    }
}
