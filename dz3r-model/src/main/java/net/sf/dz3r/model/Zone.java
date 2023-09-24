package net.sf.dz3r.model;

import net.sf.dz3r.common.HCCObjects;
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
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

/**
 * Home climate control zone.
 *
 * A {@link Thermostat} is just a device that watches the temperature.
 * A zone is an entity that controls the thermostat.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
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

    /**
     * Settings that came from the scheduler.
     *
     * {@code null} means no period is currently active. The {@link PeriodSettings#settings()} may be different from
     * {@link #settings}, this indicates that the zone is off schedule.
     */
    private PeriodSettings periodSettings;

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
        setSettingsSync(new ZoneSettings(settings, ts.getSetpoint()));

        economizer = Optional.ofNullable(economizerContext)
                .map(ctx -> new PidEconomizer<>(ts.getAddress(), ctx.settings, ctx.ambientFlux, ctx.targetDevice))
                .orElse(null);
    }

    /**
     * Set zone settings immediately.
     *
     * This method isn't deprecated, and isn't really diligent to be deprecated at the moment - however, using {@link #setSettings(ZoneSettings)} would be more diligent.
     *
     * @param settings Settings to set zone to.
     *
     * @return New settings (result of {@link ZoneSettings#merge(ZoneSettings)}).
     *
     * @throws IllegalArgumentException if things go wrong.
     */
    public ZoneSettings setSettingsSync(ZoneSettings settings) {
        HCCObjects.requireNonNull(settings, "settings can't be null");

        ts.setSetpoint(settings.setpoint);

        var newSettings = Optional.ofNullable(this.settings)
                .map(s -> s.merge(settings))
                .orElse(settings);

        var r = Integer.toHexString(newSettings.hashCode());
        logger.info("{}: setSettings({}):   {}", getAddress(), r, this.settings);
        logger.info("{}: setSettings({}): + {}", getAddress(), r, settings);
        logger.info("{}: setSettings({}): = {}", getAddress(), r, newSettings);

        this.settings = newSettings;

        return this.settings;
    }

    /**
     * Set zone settings in a reactive way.
     *
     * @param settings Settings to set zone to.
     *
     * @return The Mono signal indicating the new status or, possibly, the reason why they can't be set.
     * This mono will never be an error mono, but the wrapped {@link Signal} may be.
     */
    public Mono<Signal<ZoneSettings, String>> setSettings(ZoneSettings settings) {
        return Mono.create(sink -> {
            try {
                sink.success(new Signal<>(Instant.now(), setSettingsSync(settings), getAddress()));
            } catch (Exception ex) {
                sink.success(new Signal<>(Instant.now(), null, getAddress(), Signal.Status.FAILURE_TOTAL, ex));
            }
        });
    }

    /**
     * Set {@link #periodSettings} and {@link #settings} from the scheduler.
     *
     * @param periodSettings Settings to set. {@code null} if no period is active.
     */
    public void setPeriodSettings(PeriodSettings periodSettings) {

        this.periodSettings = periodSettings;

        if (periodSettings == null) {
            logger.info("{}: period cleared", getAddress());
            // Existing settings are left alone
            return;
        }

        var r = Integer.toHexString(settings.hashCode());

        if (Boolean.TRUE.equals(settings.hold)) {
            logger.debug("{}: setSettings({}): on hold, ignored: period = {}, settings = {}", getAddress(), r, periodSettings.period().name, periodSettings.settings());
            return;
        }

        setSettingsSync(periodSettings.settings());
        logger.info("{}: setSettings({}): period = {}", getAddress(), r, periodSettings.period().name);
    }

    public ZoneSettings getSettings() {
        return settings;
    }

    public PeriodSettings getPeriodSettings() {
        return periodSettings;
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
                new ZoneStatus(settings, source.getValue().signal, null, periodSettings),
                getAddress(),
                source.status,
                source.error);
    }

    private Signal<ZoneStatus, String> suppressIfNotEnabled(Signal<ZoneStatus, String> source) {

        if (Boolean.TRUE.equals(settings.enabled)) {
            return source;
        }

        return new Signal<>(
                source.timestamp,
                new ZoneStatus(
                        new ZoneSettings(source.getValue().settings, false),
                        new CallingStatus(null, 0, false),
                        null,
                        periodSettings),
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
