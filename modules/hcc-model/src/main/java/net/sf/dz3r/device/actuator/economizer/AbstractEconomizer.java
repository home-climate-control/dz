package net.sf.dz3r.device.actuator.economizer;

import com.homeclimatecontrol.hcc.model.EconomizerSettings;
import com.homeclimatecontrol.hcc.signal.hvac.CallingStatus;
import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.controller.HysteresisController;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.EconomizerStatus;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * Common implementation for all economizer classes.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class AbstractEconomizer implements SignalProcessor<Double, Double, String>, Addressable<String>, AutoCloseable {

    protected final Logger logger = LogManager.getLogger();

    protected final Clock clock;

    public final String name;

    private EconomizerConfig config;

    private final HvacDevice device;

    private Duration timeout;
    private final Sinks.Many<Signal<HvacCommand, Void>> deviceCommandSink = Sinks.many().unicast().onBackpressureBuffer();
    /**
     * Last known indoor temperature.
     */
    private Signal<Double, String> indoor;

    /**
     * Last known ambient temperature.
     */
    private Signal<Double, Void> ambient;

    /**
     * Economizer status.
     *
     * Can't be {@code null}, that will throw off {@link Zone#compute(Flux)} reporting.
     */
    private EconomizerStatus economizerStatus;

    /**
     * Mirrors the state of {@link #device} to avoid expensive operations.
     */
    private Boolean actuatorState;
    private FluxSink<IndoorAmbientPair> combinedSink;

    private final CountDownLatch combinedReady = new CountDownLatch(1);

    /**
     * Create an instance.
     *
     * @param device HVAC device acting as the economizer.
     * @param timeout Stale timeout. 90 seconds is a reasonable default.
     */
    protected AbstractEconomizer(
            Clock clock,
            String name,
            EconomizerConfig config,
            HvacDevice device,
            Duration timeout) {

        checkModes(config.mode, device);

        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.name = name;
        setConfig(config);

        this.device = HCCObjects.requireNonNull(device, "device can't be null");
        this.timeout = HCCObjects.requireNonNull(timeout, "timeout can't be null");

        device
                .compute(
                        deviceCommandSink
                                .asFlux()
                                .publishOn(Schedulers.boundedElastic())
                                .doOnNext(s -> logger.debug("{}: HVAC device state/send: {}", getAddress(), s))
                )
                .subscribe(s -> logger.debug("{}: HVAC device state/done: {}", getAddress(), s));

        this.economizerStatus = new EconomizerStatus(
                config.settings,
                null, 0, false, null);

        // Don't forget to connect fluxes; this can only be done in subclasses after all the
        // necessary components were initialized
        // initFluxes(ambientFlux); // NOSONAR
    }

    /**
     * Make sure the device supports the required mode.
     */
    private void checkModes(HvacMode mode, HvacDevice device) {

        if (!device.getModes().contains(mode)) {
            throw new IllegalArgumentException("requested mode " + mode + " is not among available: " + device.getModes());
        }
    }

    public void setConfig(EconomizerConfig config) {

        ThreadContext.push("setConfig");

        try {
            if (config == null) {
                throw new IllegalArgumentException("config can't be null");
            }

            this.config = this.config == null ? config : this.config.merge(config);

            logger.info("{}: setConfig(): {}", getAddress(), config);

        } finally {
            ThreadContext.pop();
        }
    }

    public void setSettings(EconomizerSettings settings) {

        ThreadContext.push("setSettings");

        try {

            if (config == null) {
                throw new IllegalStateException("setSettings() before setConfig()???");
            }

            if (settings == null) {
                throw new IllegalArgumentException("settings can't be null");
            }

            // VT: FIXME: Config and settings need to be separated

            config = config.merge(settings);

            logger.info("{}: setSettings(): {}", getAddress(), config);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public String getAddress() {
        return name;
    }
    protected final void initFluxes(Flux<Signal<Double, Void>> ambientFlux) {

        // Just get the (indoor, ambient) pair flux with no nulls or errors
        var stage1 = Flux
                .create(this::connectCombined)
                .map(this::computeCombined);

        // Get the signal
        var stage2 = computeDeviceState(stage1)
                .map(this::computeDeviceState);

        // And act on it
        stage2
                // Let the transmission layer figure out the dupes, they have a better idea about what to do with them
                .doOnNext(s -> logger.debug("{}: setDeviceState/send={}", getAddress(), s))
                .doOnNext(this::setDeviceState)
                .doOnNext(s -> logger.debug("{}: setDeviceState/done={}", getAddress(), s))

                .doOnError(t -> logger.error("{}: errored out", getAddress(), t))
                .doOnComplete(() -> logger.debug("{}: completed", getAddress()))

                // VT: NOTE: Careful when testing, this will consume everything thrown at it immediately
                .subscribe();

        ambientFlux
                .doOnNext(this::recordAmbient)

                .doOnError(t -> logger.error("{}: errored out", getAddress(), t))
                .doOnComplete(() -> logger.debug("{}: completed", getAddress()))

                // VT: NOTE: Careful when testing, this will consume everything thrown at it immediately
                .subscribe();
    }

    private void setDeviceState(Boolean state) {

        var ctl = Boolean.TRUE.equals(state) ? 1.0 : 0.0;
        var signal = new Signal<HvacCommand, Void>(
                clock.instant(),
                new HvacCommand(config.mode, ctl, ctl)
        );

        logger.debug("{}: setDeviceState={}", getAddress(), signal);

        deviceCommandSink.tryEmitNext(signal);
    }

    protected abstract Flux<Signal<Boolean, ProcessController.Status<Double>>> computeDeviceState(Flux<Signal<Double, Void>> signal);

    private void recordAmbient(Signal<Double, Void> ambient) {

        this.ambient = ambient;
        combinedSink.next(new IndoorAmbientPair(indoor, ambient));
    }

    private void recordIndoor(Signal<Double, String> indoor) {

        this.indoor = indoor;
        combinedSink.next(new IndoorAmbientPair(indoor, ambient));
    }

    private void connectCombined(FluxSink<IndoorAmbientPair> sink) {
        combinedSink = sink;
        combinedReady.countDown();
        logger.debug("{}: combined sink ready", getAddress());
    }

    /**
     * Compute the device state based on the state signal.
     *
     * @param stateSignal Device state as computed by the pipeline.
     * @return State to send to the device.
     */
    private Boolean computeDeviceState(Signal<Boolean, ProcessController.Status<Double>> stateSignal) {

        var sample = stateSignal.payload == null ? null : ((HysteresisController.HysteresisStatus) stateSignal.payload).sample;
        var demand = stateSignal.payload == null ? 0 : stateSignal.payload.signal;

        economizerStatus = new EconomizerStatus(
                Optional.ofNullable(config.settings).map(EconomizerSettings::new).orElse(null),
                sample,
                demand,
                stateSignal.getValue(),
                ambient);

        var newState = stateSignal.getValue();

        // newState can't possibly be null at this point

        if (actuatorState == null || newState.compareTo(actuatorState) != 0) {

            logger.info("{}: state change: {} => {}", getAddress(), actuatorState, newState);
            actuatorState = newState;
        }

        // Not flatmapping the result so that the transmission layer can deal with dupes as they see fit

        return actuatorState;
    }

    /**
     * Compute the virtual zone signal.
     *
     * @param indoorFlux Indoor temperature flux.
     *
     * @return Flux to send to {@code UnitDirector} instead of the zone flux.
     */
    @Override
    public Flux<Signal<Double, String>> compute(Flux<Signal<Double, String>> indoorFlux) {

        // This better be ready, or we'll blow up
        acquireCombinedSink();

        // Not doing much right here - just recording the indoor signal and passing it down
        // while doing all the calculations in a side channel
        return indoorFlux.doOnNext(this::recordIndoor);
    }

    private void acquireCombinedSink() {

        logger.debug("{}: awaiting combined sink...", getAddress());
        try {

            combinedReady.await();

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
            throw new IllegalStateException("failed to acquire combined sink", ex);
        }

        logger.debug("{}: acquired combined sink", getAddress());
    }

    private Signal<Double, Void> computeCombined(IndoorAmbientPair pair) {

        ThreadContext.push("computeCombined");

        try {

            logger.trace("{}: raw {}", getAddress(), pair);

            // Shortcut: are we enabled to begin with?

            if (!config.isEnabled()) {

                logger.trace("not enabled, bailing out with 0: {}", config);
                return new Signal<>(clock.instant(), 0d);
            }

            // Let's take care of corner cases first

            if (pair.indoor == null || pair.ambient == null) {

                // No go, incomplete information
                logger.debug("{}: null signals? {}", getAddress(), pair);
                return new Signal<>(clock.instant(), null, null, Signal.Status.FAILURE_TOTAL, new IllegalStateException("null signals, see the log above"));
            }

            if (pair.indoor.isError() || pair.ambient.isError()) {

                // Absolutely not
                logger.warn("{}: error signals? {}", getAddress(), pair);
                return new Signal<>(clock.instant(), null, null, Signal.Status.FAILURE_TOTAL, new IllegalStateException("error signals, see the log above"));
            }

            // Let's be generous; Zigbee sensors can fall back to 60 seconds interval even if configured faster,
            // and even 90 seconds can cause blips once in a while
            var stale = clock.instant().minus(timeout);

            if (pair.indoor.timestamp.isBefore(stale) || pair.ambient.timestamp.isBefore(stale)) {

                // How??? Stale signals should have been taken care of by the TimeoutGuard by now.
                logger.error("{}: stale signals? resetting both {}", getAddress(), pair);

                this.indoor = null;
                this.ambient = null;

                return new Signal<>(clock.instant(), -1d);
            }

            var indoorTemperature = pair.indoor.getValue();
            var ambientTemperature = pair.ambient.getValue();

            var signal = computeCombined(indoorTemperature, ambientTemperature);

            // The latter one wins
            var timestamp = indoor.timestamp.isAfter(ambient.timestamp) ? indoor.timestamp : ambient.timestamp;

            return new Signal<>(timestamp, signal);

        } finally {
            ThreadContext.pop();
        }
    }

    protected double computeCombined(Double indoorTemperature, Double ambientTemperature) {

        logger.debug("{}: valid indoor={}, ambient={}", getAddress(), indoorTemperature, ambientTemperature);

        // Adjusted for mode
        var ambientDelta = getAmbientDelta(indoorTemperature, ambientTemperature);
        var targetDelta = getTargetDelta(indoorTemperature);

        double targetAdjustment;

        if (targetDelta > config.settings.changeoverDelta()) {

            // We're still above the target
            targetAdjustment = 0.0;

        } else if (ambientDelta < 0) {

            // We're below the target, but the mode adjusted ambient is still too high. Happens.
            targetAdjustment = 0.0;

        } else {

            // As the indoor temperature is approaching the target, need to take corrective measures
            var k = (1.0 / config.settings.changeoverDelta()) * (config.settings.changeoverDelta() - targetDelta);

            targetAdjustment = ambientDelta * k;

            logger.debug("{}: k={}, targetAdjustment={}", getAddress(), k, targetAdjustment);
        }

        var signal = ambientDelta - targetAdjustment;

        logger.debug("{}: ambientD={}, targetD={}, signal={}", getAddress(), ambientDelta, targetDelta, signal);

        return signal;
    }

    /**
     * Get the {@link EconomizerSettings#changeoverDelta()}  ambient} delta signal.
     *
     * @return Positive value indicates demand, negative indicates lack thereof, regardless of mode.
     */
    double getAmbientDelta(double indoor, double ambient) {

        return config.mode == HvacMode.COOLING
                ? indoor - (ambient + config.settings.changeoverDelta())
                : ambient - (indoor + config.settings.changeoverDelta());
    }

    /**
     * Get the {@link EconomizerSettings#targetTemperature()} delta signal.
     *
     * @return Positive value indicates demand, negative indicates lack thereof, regardless of mode.
     */
    double getTargetDelta(double indoor) {
        return config.mode == HvacMode.COOLING
                ? indoor - config.settings.targetTemperature()
                : config.settings.targetTemperature() - indoor;
    }

    /**
     * Figure out whether the HVAC needs to be suppressed and adjust the signal if so.
     *
     * @param source Signal computed by {@link Zone}.
     * @return Signal with {@link ZoneStatus#callingStatus()} possibly adjusted to shut off the HVAC if the economizer is active.
     */
    public Signal<ZoneStatus, String> computeHvacSuppression(Signal<ZoneStatus, String> source) {

        if (source.isError()) {

            // How did it get here? This log message is likely a dupe, need to keep an eye on it
            logger.warn("{}: error signal in economizer pipeline? {}", getAddress(), source);

            // Can't do anything meaningful with it here
            return source;
        }

        var zoneSettings = source.getValue();

        // Augment the source with the economizer status
        var augmentedSource = new Signal<>(
                source.timestamp,
                new ZoneStatus(
                        zoneSettings.settings(),
                        zoneSettings.callingStatus(),
                        economizerStatus,
                        zoneSettings.periodSettings()),
                source.payload,
                source.status,
                source.error);

        if (actuatorState == null || actuatorState.equals(Boolean.FALSE)) {

            // Economizer inactive, no change required
            return augmentedSource;
        }

        if (config.settings.isKeepHvacOn()) {

            // We're feeding indoor air to HVAC air return, right?
            return augmentedSource;
        }

        // Need to suppress demand and keep the HVAC off while the economizer is on
        var adjusted = new ZoneStatus(
                zoneSettings.settings(),
                new CallingStatus(null, 0, false),
                economizerStatus,
                zoneSettings.periodSettings());

        return new Signal<>(
                source.timestamp,
                adjusted,
                source.payload,
                source.status,
                source.error);
    }

    @Override
    public void close() throws Exception {
        ThreadContext.push("close");
        try {
            logger.warn("Shutting down: {}", getAddress());
            setDeviceState(false);
            deviceCommandSink.tryEmitComplete();
        } finally {
            logger.info("Shut down: {}", getAddress());
            ThreadContext.pop();
        }
    }

    private record IndoorAmbientPair(
            Signal<Double, String> indoor,
            Signal<Double, Void> ambient
    ) {

    }
}
