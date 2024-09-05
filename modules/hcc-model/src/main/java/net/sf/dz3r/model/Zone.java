package net.sf.dz3r.model;

import com.homeclimatecontrol.hcc.model.ZoneSettings;
import com.homeclimatecontrol.hcc.signal.hvac.CallingStatus;
import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.actuator.economizer.AbstractEconomizer;
import net.sf.dz3r.device.actuator.economizer.EconomizerContext;
import net.sf.dz3r.device.actuator.economizer.v2.PidEconomizer;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Home climate control zone.
 *
 * A {@link Thermostat} is just a device that watches the temperature.
 * A zone is an entity that controls the thermostat.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class Zone implements SignalProcessor<Double, ZoneStatus, String>, Addressable<String>, AutoCloseable {

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
    private com.homeclimatecontrol.hcc.model.ZoneSettings settings;

    /**
     * Settings that came from the scheduler.
     *
     * {@code null} means no period is currently active. The {@link PeriodSettings#settings()} may be different from
     * {@link #settings}, this indicates that the zone is off schedule.
     */
    private PeriodSettings periodSettings;

    private final AbstractEconomizer economizer;

    private final Sinks.Many<Signal<Double, String>> feedbackSink = Sinks.many().unicast().onBackpressureBuffer();
    private final Flux<Signal<Double, String>> feedbackFlux = feedbackSink.asFlux();

    private Signal<Double, String> lastKnownSignal;

    /**
     * Create an instance without an economizer.
     *
     * @param ts Thermostat to use.
     * @param settings Zone settings. Thermostat setpoint overrides zone setpoint - this argument is here to configure initial flags.
     */
    public Zone(Thermostat ts, com.homeclimatecontrol.hcc.model.ZoneSettings settings) {
        this(ts, settings, null);
    }

    /**
     * Create an instance with an economizer.
     *
     * @param ts Thermostat to use.
     * @param settings Zone settings. Thermostat setpoint overrides zone setpoint - this argument is here to configure initial flags.
     * @param economizerContext Optional context to initialize the economizer with.
     */
    public Zone(Thermostat ts, com.homeclimatecontrol.hcc.model.ZoneSettings settings, EconomizerContext economizerContext) {
        this.ts = ts;
        setSettingsSync(new ZoneSettings(settings, settings.setpoint()));

        economizer = Optional.ofNullable(economizerContext)
                .map(ctx -> new PidEconomizer<>(
                        Clock.systemUTC(),
                        ts.getAddress(),
                        ctx.config(),
                        ctx.ambientFlux(),
                        ctx.device(),
                        ctx.timeout()))
                .orElse(null);
    }

    /**
     * Set zone settings immediately.
     *
     * This method isn't deprecated, and isn't really diligent to be deprecated at the moment - however, using {@link #setSettings(com.homeclimatecontrol.hcc.model.ZoneSettings)} would be more diligent.
     *
     * @param settings Settings to set zone to.
     *
     * @return New settings (result of {@link com.homeclimatecontrol.hcc.model.ZoneSettings#merge(com.homeclimatecontrol.hcc.model.ZoneSettings)}).
     *
     * @throws IllegalArgumentException if things go wrong.
     */
    public com.homeclimatecontrol.hcc.model.ZoneSettings setSettingsSync(com.homeclimatecontrol.hcc.model.ZoneSettings settings) {
        HCCObjects.requireNonNull(settings, "settings can't be null");

        ts.setSetpoint(settings.setpoint());

        var newSettings = Optional.ofNullable(this.settings)
                .map(s -> s.merge(settings))
                .orElse(settings);

        var r = Integer.toHexString(newSettings.hashCode());
        logger.debug("{}: setSettings({}):   {}", getAddress(), r, this.settings);
        logger.debug("{}: setSettings({}): + {}", getAddress(), r, settings);
        logger.info("{}: setSettings({}): = {}", getAddress(), r, newSettings);

        if (economizer != null) {
            economizer.setSettings(newSettings.economizerSettings());
        }

        this.settings = newSettings;

        bump();

        return this.settings;
    }

    /**
     * Force the {@link #lastKnownSignal} through {@link #compute(Flux)}.
     *
     * Note that there's one replay in {@link net.sf.dz3r.controller.AbstractProcessController#setSetpoint(double)}
     * (which will cause the signal to be replayed twice), however, settings outside the process controller may have changed
     * which makes this necessary.
     */
    private void bump() {

        if (lastKnownSignal == null) {
            logger.debug("{}: no lastKnownSignal yet, settings will get to consumers on next sensor signal arrival", getAddress());
            return;
        }

        logger.debug("{}: replaying signal: {}", getAddress(), lastKnownSignal);
        feedbackSink.tryEmitNext(lastKnownSignal);
    }

    /**
     * Set zone settings in a reactive way.
     *
     * @param settings Settings to set zone to.
     *
     * @return The Mono signal indicating the new status or, possibly, the reason why they can't be set.
     * This mono will never be an error mono, but the wrapped {@link Signal} may be.
     */
    public Mono<Signal<com.homeclimatecontrol.hcc.model.ZoneSettings, String>> setSettings(com.homeclimatecontrol.hcc.model.ZoneSettings settings) {
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

        if (settings.isOnHold()) {
            logger.debug("{}: setSettings({}): on hold, ignored: period = {}, settings = {}", getAddress(), r, periodSettings.period().name, periodSettings.settings());
            return;
        }

        setSettingsSync(periodSettings.settings());
        logger.info("{}: setSettings({}): period = {}", getAddress(), r, periodSettings.period().name);
    }

    public com.homeclimatecontrol.hcc.model.ZoneSettings getSettings() {
        return settings;
    }

    public PeriodSettings getPeriodSettings() {
        return periodSettings;
    }

    @Override
    public Flux<Signal<ZoneStatus, String>> compute(Flux<Signal<Double, String>> in) {

        // There are two input streams:
        //
        // - the argument, brings in sensor readings
        // - the feedback, repeats the last sensor reading upon changing zone settings and forces a new signal to be emitted here

        // Record the signal so the feedback flux can pick it up
        var recorded = in
                .doOnNext(this::recordSignal)
                .doOnComplete(feedbackSink::tryEmitComplete);

        var combined = Flux.merge(recorded, feedbackFlux);

        var source = Optional.ofNullable(economizer)
                .map(eco -> eco.compute(combined))
                .orElse(combined);

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
                .doOnNext(e -> logger.trace("compute {}/translated: {}", getAddress(), e));

        // Now, dampen the signal if the zone is disabled
        var stage3 = stage2
                .map(this::suppressIfNotEnabled)
                .doOnNext(e -> logger.trace("compute {}/isOn: {} {}", getAddress(), settings.isEnabled() ? "enabled" : "DISABLED", e));

        // And finally, suppress if the economizer says so
        return stage3.map(this::suppressEconomizer);
    }

    private void recordSignal(Signal<Double, String> signal) {
        this.lastKnownSignal = signal;
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

        if (Boolean.TRUE.equals(settings.isEnabled())) {
            return source;
        }

        return new Signal<>(
                source.timestamp,
                new ZoneStatus(
                        new ZoneSettings(source.getValue().settings(), false),
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
    public String toString() {
        return "{zone name=" + ts.getAddress() + ", settings={" + settings + "}, range={" + ts.setpointRange + "}}";
    }

    public Range<Double> getSetpointRange() {
        return ts.setpointRange;
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

    public void raise() {
        ts.raise();
    }
}
