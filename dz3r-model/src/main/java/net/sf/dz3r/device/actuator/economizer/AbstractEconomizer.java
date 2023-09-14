package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.controller.HysteresisController;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.actuator.StackingSwitch;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.CallingStatus;
import net.sf.dz3r.signal.hvac.EconomizerStatus;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;

/**
 * Common implementation for all economizer classes.
 *
 * @param <A> Actuator device address type.
 */
public abstract class AbstractEconomizer <A extends Comparable<A>> implements SignalProcessor<Double, Double, String>, Addressable<String>, AutoCloseable {

    protected final Logger logger = LogManager.getLogger();

    public final String name;

    public EconomizerSettings settings;

    private final Switch<A> targetDevice;

    private final StackingSwitch targetDeviceStack;

    /**
     * Last known indoor temperature. Can't be an error.
     */
    private Signal<Double, String> indoor;

    /**
     * Last known ambient temperature. Can't be an error.
     */
    private Signal<Double, Void> ambient;

    /**
     * Economizer status.
     *
     * Can't be {@code null}, that will throw off {@link Zone#compute(Flux)} reporting.
     */
    private EconomizerStatus economizerStatus;

    /**
     * Mirrors the state of {@link #targetDevice} to avoid expensive operations.
     */
    private Boolean actuatorState;
    private FluxSink<Pair<Signal<Double, String>, Signal<Double, Void>>> combinedSink;

    private final CountDownLatch combinedReady = new CountDownLatch(1);

    /**
     * Create an instance.
     *
     * Note that only the {@code ambientFlux} argument is present, indoor flux is provided to {@link #compute(Flux)}.
     *
     * @param ambientFlux Flux from the ambient temperature sensor.
     * @param targetDevice Switch to control the economizer actuator.
     */
    protected AbstractEconomizer(
            String name,
            EconomizerSettings settings,
            Flux<Signal<Double, Void>> ambientFlux,
            Switch<A> targetDevice) {

        this.name = name;
        this.settings = settings;

        this.targetDevice = targetDevice;

        // There are two virtual switches linked to this switch:
        //
        // "demand" - controlled by heating and cooling operations
        // "ventilation" - controlled by explicit requests to turn the fan on or off

        this.targetDeviceStack = new StackingSwitch("economizer", targetDevice);

        this.economizerStatus = new EconomizerStatus(
                new EconomizerTransientSettings(settings),
                null, 0, false, null);

        // Don't forget to connect fluxes; this can only be done in subclasses after all the
        // necessary components were initialized
        // initFluxes(ambientFlux); // NOSONAR
    }

    public void setSettings(EconomizerSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("settings can't be null");
        }

        this.settings = this.settings == null ? settings : this.settings.merge(settings);

        logger.info("{}: setSettings(): {}", getAddress(), settings);
    }

    @Override
    public String getAddress() {
        return name;
    }
    protected final void initFluxes(Flux<Signal<Double, Void>> ambientFlux) {

        // Just get the (indoor, ambient) pair flux with no nulls or errors
        var stage1 = Flux
                .create(this::connectCombined)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(pair -> logger.debug("{}: raw indoor={}, ambient={}", getAddress(), pair.getLeft(), pair.getRight()))
                .filter(pair -> pair.getLeft() != null && pair.getRight() != null)
                .filter(pair -> !pair.getLeft().isError() && !pair.getRight().isError())
                .map(this::computeCombined);

        // Get the signal
        var stage2 = computeDeviceState(stage1)
                .map(this::recordDeviceState);

        var demandDevice = targetDeviceStack.getSwitch("demand");

        // And act on it
        stage2
                // Let the transmission layer figure out the dupes, they have a better idea about what to do with them
                .flatMap(demandDevice::setState)

                // VT: NOTE: Careful when testing, this will consume everything thrown at it immediately
                .subscribe();

        ambientFlux
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(this::recordAmbient)

                // VT: NOTE: Careful when testing, this will consume everything thrown at it immediately
                .subscribe();
    }

    protected abstract Flux<Signal<Boolean, ProcessController.Status<Double>>> computeDeviceState(Flux<Signal<Double, Void>> signal);

    private void recordAmbient(Signal<Double, Void> ambient) {

        this.ambient = ambient;
        combinedSink.next(new ImmutablePair<>(indoor, ambient));
    }

    private void recordIndoor(Signal<Double, String> indoor) {

        this.indoor = indoor;
        combinedSink.next(new ImmutablePair<>(indoor, ambient));
    }

    private void connectCombined(FluxSink<Pair<Signal<Double, String>, Signal<Double, Void>>> sink) {
        combinedSink = sink;
        combinedReady.countDown();
        logger.debug("{}: combined sink ready", getAddress());
    }

    private Boolean recordDeviceState(Signal<Boolean, ProcessController.Status<Double>> stateSignal) {

        var sample = stateSignal.payload == null ? null : ((HysteresisController.HysteresisStatus) stateSignal.payload).sample;
        var demand = stateSignal.payload == null ? 0 : stateSignal.payload.signal;

        economizerStatus = new EconomizerStatus(
                new EconomizerTransientSettings(settings),
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

    private Signal<Double, Void> computeCombined(Pair<Signal<Double, String>, Signal<Double, Void>> pair) {

        ThreadContext.push("computeCombined");

        try {

            // Null and error values have already been filtered out
            var indoorTemperature = pair.getLeft().getValue();
            var ambientTemperature = pair.getRight().getValue();

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

        if (targetDelta > settings.changeoverDelta) {

            // We're still above the target
            targetAdjustment = 0.0;

        } else if (ambientDelta < 0) {

            // We're below the target, but the mode adjusted ambient is too high - this is an abnormal situation,
            // either the target is misconfigured, or someone pulled the setpoint too far

            logger.warn("{}: economizer abnormal, indoor={}, ambient={}, settings={}", getAddress(), indoorTemperature, ambientTemperature, settings);
            targetAdjustment = 0.0;

        } else {

            // As the indoor temperature is approaching the target, need to take corrective measures
            var k = (1.0 / settings.changeoverDelta) * (settings.changeoverDelta - targetDelta);

            targetAdjustment = ambientDelta * k;

            logger.debug("{}: k={}, targetAdjustment={}", getAddress(), k, targetAdjustment);
        }

        var signal = ambientDelta - targetAdjustment;

        logger.debug("{}: ambientD={}, targetD={}, signal={}", getAddress(), ambientDelta, targetDelta, signal);

        return signal;
    }

    /**
     * Get the {@link EconomizerSettings#changeoverDelta ambient} delta signal.
     *
     * @return Positive value indicates demand, negative indicates lack thereof, regardless of mode.
     */
    double getAmbientDelta(double indoor, double ambient) {

        return settings.mode == HvacMode.COOLING
                ? indoor - (ambient + settings.changeoverDelta)
                : ambient - (indoor + settings.changeoverDelta);
    }

    /**
     * Get the {@link EconomizerSettings#targetTemperature} delta signal.
     *
     * @return Positive value indicates demand, negative indicates lack thereof, regardless of mode.
     */
    double getTargetDelta(double indoor) {
        return settings.mode == HvacMode.COOLING
                ? indoor - settings.targetTemperature
                : settings.targetTemperature - indoor;
    }

    /**
     * Figure out whether the HVAC needs to be suppressed and adjust the signal if so.
     *
     * @param source Signal computed by {@link Zone}.
     * @return Signal with {@link ZoneStatus#callingStatus} possibly adjusted to shut off the HVAC if the economizer is active.
     */
    public Signal<ZoneStatus, String> computeHvacSuppression(Signal<ZoneStatus, String> source) {

        if (source.isError()) {

            // How did it get here? This log message is likely a dupe, need to keep an eye on it
            logger.warn("{}: error signal in economizer pipeline? {}", getAddress(), source);

            // Can't do anything meaningful with it here
            return source;
        }

        // Augment the source with the economizer status
        var augmentedSource = new Signal<>(
                source.timestamp,
                new ZoneStatus(
                        source.getValue().settings,
                        source.getValue().callingStatus,
                        economizerStatus),
                source.payload,
                source.status,
                source.error);

        if (actuatorState == null || actuatorState.equals(Boolean.FALSE)) {

            // Economizer inactive, no change required
            return augmentedSource;
        }

        if (settings.keepHvacOn) {

            // We're feeding indoor air to HVAC air return, right?
            return augmentedSource;
        }

        // Need to suppress demand and keep the HVAC off while the economizer is on
        var adjusted = new ZoneStatus(
                source.getValue().settings,
                new CallingStatus(null, 0, false),
                economizerStatus);

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
            targetDevice.setState(false).block();
        } finally {
            logger.info("Shut down: {}", getAddress());
            ThreadContext.pop();
        }
    }
}
