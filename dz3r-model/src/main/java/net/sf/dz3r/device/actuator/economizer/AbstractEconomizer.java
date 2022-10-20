package net.sf.dz3r.device.actuator.economizer;

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

/**
 * Common implementation for all economizer classes.
 *
 * @param <A> Actuator device address type.
 */
public abstract class AbstractEconomizer <A extends Comparable<A>> implements SignalProcessor<Double, ZoneStatus, String> {

    protected final Logger logger = LogManager.getLogger();

    public final EconomizerSettings config;

    public final Zone targetZone;

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

    /**
     * Create an instance.
     *
     * Note that only the {@code ambientFlux} argument is present, indoor flux is provided to {@link #compute(Flux)}.
     *
     * @param targetZone Zone to serve.
     * @param ambientFlux Flux from the ambient temperature sensor.
     * @param targetDevice Switch to control the economizer actuator.
     */
    protected AbstractEconomizer(
            EconomizerSettings settings,
            Zone targetZone,
            Flux<Signal<Double, Void>> ambientFlux,
            Switch<A> targetDevice) {

        this.config = settings;
        this.targetZone = targetZone;
        this.targetDevice = targetDevice;

        // Don't forget to connect fluxes; this can only be done in subclasses after all the
        // necessary components were initialized
        // initFluxes(ambientFlux); // NOSONAR
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
                .doOnNext(this::recordDeviceState);

        // And act on it
        stage2
                // Let the transmission layer figure out the dupes, they have a better idea about what to do with them
                .flatMap(state -> targetDevice.setState(actuatorState))

                // VT: NOTE: Careful when testing, this will consume everything thrown at it immediately
                .subscribe();

        ambientFlux
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(this::recordAmbient)

                // VT: NOTE: Careful when testing, this will consume everything thrown at it immediately
                .subscribe();
    }

    protected abstract Flux<Signal<Boolean, Void>> computeDeviceState(Flux<Signal<Double, Void>> signal);

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
    }

    private void recordDeviceState(Signal<Boolean, Void> stateSignal) {

        var newState = stateSignal.getValue();

        if ((actuatorState == null && newState != null) || newState.compareTo(actuatorState) != 0) {

            logger.info("{}: eco state change: {} => {}", targetZone.getAddress(), actuatorState, newState);
            actuatorState = newState;
        }
    }

    /**
     * Compute the virtual zone signal.
     *
     * @param indoorFlux Indoor temperature flux.
     *
     * @return Flux to send to {@code UnitDirector} instead of the zone flux.
     */
    @Override
    public Flux<Signal<ZoneStatus, String>> compute(Flux<Signal<Double, String>> indoorFlux) {

        // Summary of what we need to do here:

        // - compare the indoor flux against the changeover and target temperature, and
        //   issue a control signal when the actuator needs to be on
        // - feed the indoor flux to the zone, intercept the zone output, and suppress demand there
        //   when the actuator is on (if so configured)

        indoorFlux
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(this::recordIndoor)

                // VT: NOTE: Careful when testing, this will consume everything thrown at it immediately
                .subscribe();

        return targetZone
                .compute(indoorFlux)
                .map(this::computeHvacSuppression);
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

        return config.mode == HvacMode.COOLING
                ? indoor - (ambient + config.changeoverDelta)
                : ambient - (indoor + config.changeoverDelta);
    }

    /**
     * Get the {@link EconomizerSettings#targetTemperature} delta signal.
     *
     * @return Positive value indicates demand, negative indicates lack thereof, regardless of mode.
     */
    double getTargetDelta(double indoor) {
        return config.mode == HvacMode.COOLING
                ? indoor - config.targetTemperature
                : config.targetTemperature - indoor;
    }

    /**
     * Figure out whether the HVAC needs to be suppressed and adjust the signal if so.
     *
     * @param source Signal computed by {@link Zone}.
     * @return Signal with {@link ZoneStatus#status} possibly adjusted to shut off the HVAC if the economizer is active.
     */
    private Signal<ZoneStatus, String> computeHvacSuppression(Signal<ZoneStatus, String> source) {

        if (actuatorState == null || actuatorState == Boolean.FALSE) {

            // Economizer inactive, no change required
            return source;
        }

        if (config.keepHvacOn) {

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
}
