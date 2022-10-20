package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.ThermostatStatus;
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

    public final EconomizerSettings settings;

    protected final Switch<A> targetDevice;

    /**
     * Last known indoor temperature. Can't be an error.
     */
    private Signal<Double, String> indoor;

    /**
     * Last known ambient temperature. Can't be an error.
     */
    private Signal<Double, Void> ambient;

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
            EconomizerSettings settings,
            Flux<Signal<Double, Void>> ambientFlux,
            Switch<A> targetDevice) {

        this.settings = settings;
        this.targetDevice = targetDevice;

        // Don't forget to connect fluxes; this can only be done in subclasses after all the
        // necessary components were initialized
        // initFluxes(ambientFlux); // NOSONAR
    }

    @Override
    public String getAddress() {
        return settings.name;
    }
    protected final void initFluxes(Flux<Signal<Double, Void>> ambientFlux) {

        // Just get the (indoor, ambient) pair flux with no nulls or errors
        var stage1 = Flux
                .create(this::connectCombined)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(pair -> logger.debug("raw indoor={}, ambient={}", pair.getLeft(), pair.getRight()))
                .filter(pair -> pair.getLeft() != null && pair.getRight() != null)
                .filter(pair -> !pair.getLeft().isError() && !pair.getRight().isError())
                .map(this::computeCombined);

        // Get the signal
        var stage2 = computeDeviceState(stage1)
                .map(this::recordDeviceState);

        // And act on it
        stage2
                // Let the transmission layer figure out the dupes, they have a better idea about what to do with them
                .flatMap(targetDevice::setState)

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
        logger.debug("combined sink ready");
    }

    private Boolean recordDeviceState(Signal<Boolean, ProcessController.Status<Double>> stateSignal) {

        var newState = stateSignal.getValue();

        if ((actuatorState == null && newState != null) || newState.compareTo(actuatorState) != 0) {

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

        logger.debug("awaiting combined sink...");
        try {

            combinedReady.await();

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
            throw new IllegalStateException("failed to acquire combined sink", ex);
        }

        logger.debug("acquired combined sink");
    }

    private Signal<Double, Void> computeCombined(Pair<Signal<Double, String>, Signal<Double, Void>> pair) {

        ThreadContext.push("computeCombined");

        try {

            // Null and error values have already been filtered out
            var indoorTemperature = pair.getLeft().getValue();
            var ambientTemperature = pair.getRight().getValue();

            logger.debug("valid indoor={}, ambient={}", indoorTemperature, ambientTemperature);

            // Adjusted for mode
            var ambientDelta = getAmbientDelta(indoorTemperature, ambientTemperature);
            var targetDelta = getTargetDelta(indoorTemperature);

            // Negative target data indicates indoor temperature beyond threshold; ambient is irrelevant at this point,
            // economizer needs to be shut off
            var signal = targetDelta > 0
                    ? ambientDelta
                    : targetDelta;

            logger.debug("ambientD={}, targetD={}, signal={}", ambientDelta, targetDelta, signal);

            // The latter one wins
            var timestamp = indoor.timestamp.isAfter(ambient.timestamp) ? indoor.timestamp : ambient.timestamp;

            return new Signal<>(timestamp, signal);

        } finally {
            ThreadContext.pop();
        }
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
     * @return Signal with {@link ZoneStatus#status} possibly adjusted to shut off the HVAC if the economizer is active.
     */
    public Signal<ZoneStatus, String> computeHvacSuppression(Signal<ZoneStatus, String> source) {

        if (actuatorState == null || actuatorState == Boolean.FALSE) {

            // Economizer inactive, no change required
            return source;
        }

        if (settings.keepHvacOn) {

            // We're feeding indoor air to HVAC air return, right?
            return source;
        }

        if (source.isError()) {

            // How did it get here? This log message is likely a dupe, need to keep an eye on it
            logger.warn("error signal in economizer pipeline? {}", source);

            // Can't do anything meaningful with it here
            return source;
        }

        // Need to suppress demand and keep the HVAC off while the economizer is on
        var adjusted = new ZoneStatus(
                source.getValue().settings,
                new ThermostatStatus(0, false));

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
            logger.info("Shutting down");
            targetDevice.setState(false).block();
        } finally {
            logger.info("Shut down");
            ThreadContext.pop();
        }
    }
}
