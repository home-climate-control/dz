package net.sf.dz3r.model;

import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.actuator.economizer.AbstractEconomizer;
import net.sf.dz3r.device.actuator.economizer.EconomizerContext;
import net.sf.dz3r.device.actuator.economizer.v2.PidEconomizer;
import net.sf.dz3r.jmx.JmxAware;
import net.sf.dz3r.jmx.JmxDescriptor;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.CallingStatus;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * Home climate control zone.
 *
 * A {@link Thermostat} is just a device that watches the temperature.
 * A zone is an entity that controls the thermostat.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class Zone implements SignalProcessor<Double, ZoneStatus, String>, Addressable<String>, JmxAware, AutoCloseable {

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

    private final AbstractEconomizer<?> economizer;

    /**
     * Create an instance without an economizer.
     *
     * @param ts Thermostat to use.
     * @param settings Zone settings. Thermostat setpoint overrides zone setpoint - this argument is here to configure initial flags.
     */
    public Zone(Thermostat ts, ZoneSettings settings) {
        this(ts, settings, null);
    }

    /**
     * Create an instance with an economizer.
     *
     * @param ts Thermostat to use.
     * @param settings Zone settings. Thermostat setpoint overrides zone setpoint - this argument is here to configure initial flags.
     * @param economizerContext Optional context to initialize the economizer with.
     */
    public Zone(Thermostat ts, ZoneSettings settings, EconomizerContext<?> economizerContext) {
        this.ts = ts;
        setSettings(new ZoneSettings(settings, ts.getSetpoint()));

        economizer = Optional.ofNullable(economizerContext)
                .map(ctx -> new PidEconomizer<>(ts.getAddress(), ctx.settings, ctx.ambientFlux, ctx.targetDevice))
                .orElse(null);
    }

    public void setSettings(ZoneSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("settings can't be null");
        }

        ts.setSetpoint(settings.setpoint);

        this.settings = this.settings == null ? settings : this.settings.merge(settings);

        logger.info("{}: setSettings(): {}", getAddress(), settings);
    }

    public ZoneSettings getSettings() {
        return settings;
    }

    @Override
    public Flux<Signal<ZoneStatus, String>> compute(Flux<Signal<Double, String>> in) {

        var source = Optional.ofNullable(economizer)
                .map(eco -> eco.compute(in))
                .orElse(in);

        // Since the zone doesn't need the payload, but the thermostat does, need to translate the input
        var stage0 = source
                .map(s -> new Signal<>(s.timestamp, s.getValue(), (Void) null, s.status, s.error));

        // Not to disrupt the thermostat control logic, input signal is fed to it
        // regardless of whether the zone is enabled
        var stage1 = ts
                .compute(stage0)
                .doOnNext(e -> logger.trace("compute {}/ts: {}", getAddress(), e));

        // Now, need to translate into a form that is easier manipulated
        var stage2 = stage1.map(this::translate)
                .doOnNext(e -> logger.debug("compute {}/translated: {}", getAddress(), e));

        // Now, dampen the signal if the zone is disabled
        var stage3 = stage2
                .map(this::suppressIfNotEnabled)
                .doOnNext(e -> logger.debug("compute {}/isOn: {} {}", getAddress(), settings.enabled ? "enabled" : "DISABLED", e));

        // And finally, suppress if the economizer says so
        return stage3.map(this::suppressEconomizer);
    }

    private Signal<ZoneStatus, String> translate(Signal<ProcessController.Status<CallingStatus>, Void> source) {

        return new Signal<>(
                source.timestamp,
                new ZoneStatus(settings, source.getValue().signal, null),
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
                new ZoneStatus(new ZoneSettings(source.getValue().settings, false), new CallingStatus(null, 0, false), null),
                source.payload,
                source.status,
                source.error);
    }

    private Signal<ZoneStatus, String> suppressEconomizer(Signal<ZoneStatus, String> source) {

        return Optional.ofNullable(economizer)
                .map(eco -> eco.computeHvacSuppression(source))
                .orElse(source);
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

    @Override
    public void close() throws Exception {
        ThreadContext.push("close");
        try {

            logger.warn("Shutting down: {}", getAddress());

            if (economizer != null) {
                economizer.close();
            }

        } finally {
            logger.info("Shut down: {}", getAddress());
            ThreadContext.pop();
        }
    }
}
