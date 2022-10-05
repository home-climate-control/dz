package net.sf.dz3r.device.actuator.economizer.v1;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.ThermostatStatus;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Economizer implementation.
 *
 * More information: <a href="https://github.com/home-climate-control/dz/wiki/HVAC-Device:-Economizer">HVAC Device: Economizer</a>
 *
 * @param <A> Actuator device address type.
 */
public class Economizer<A extends Comparable<A>> implements SignalProcessor<Double, ZoneStatus, String>, Addressable<String> {

    private final Logger logger = LogManager.getLogger();

    public final String name;
    public final EconomizerConfig config;

    public final Zone targetZone;

    /**
     * Last known indoor temperature. Can't be an error.
     */
    private Signal<Double, String> indoor;

    /**
     * Last known ambient temperature. Can't be an error.
     */
    private Signal<Double, Void> ambient;

    private final Switch<A> targetDevice;

    /**
     * Mirrors the state of {@link #targetDevice} to avoid expensive operations.
     */
    private Boolean actuatorState;

    /**
     * Create an instance.
     *
     * Note that only the {@code ambientFlux} argument is present, indoor flux is provided to {@link #compute(Flux)}.
     *
     * @param name Human readable name.
     * @param targetZone Zone to serve.
     * @param ambientFlux Flux from the ambient temperature sensor.
     * @param targetDevice Switch to control the economizer actuator.
     */
    public Economizer(
            String name,
            EconomizerConfig config,
            Zone targetZone,
            Flux<Signal<Double, Void>> ambientFlux,
            Switch<A> targetDevice) {

        this.name = name;
        this.config = config;
        this.targetZone = targetZone;
        this.targetDevice = targetDevice;

        ambientFlux
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::computeAmbient)
                .doOnNext(this::recordDeviceState)

                // Let the transmission layer figure out the dupes, they have a better idea about what to do with them
                .flatMap(state -> targetDevice.setState(actuatorState))

                // VT: NOTE: Careful when testing, this will consume everything thrown at it immediately
                .subscribe();
    }

    private void recordDeviceState(Signal<Boolean, Void> stateSignal) {

        var newState = stateSignal.getValue();

        if ((actuatorState == null && newState != null) || newState.compareTo(actuatorState) != 0) {

            logger.info("{}: state change: {} => {}", name, actuatorState, newState);
            actuatorState = newState;
        }
    }

    @Override
    public String getAddress() {
        return name;
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
        //   when the actuator is on

        indoorFlux
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::computeIndoor)
                .doOnNext(this::recordDeviceState)

                // Let the transmission layer figure out the dupes, they have a better idea about what to do with them
                .flatMap(state -> targetDevice.setState(actuatorState))

                // VT: NOTE: Careful when testing, this will consume everything thrown at it immediately
                .subscribe();

        return targetZone
                .compute(indoorFlux)
                .map(this::computeZone);
    }

    /**
     * Aggregate with {@link #ambient} and pass control to {@link #computeDeviceState(Signal, Signal)}.
     *
     * @return Empty flux if it isn't possible to compute the value, or the target device to execute.
     */
    private Flux<Signal<Boolean, Void>> computeIndoor(Signal<Double, String> indoor) {

        ThreadContext.push("computeIndoor");

        try {

            logger.debug("indoor={}", indoor);

            if (indoor.isError()) {

                // Can't do much here other than shut the economizer off, who knows what's going on there
                return Flux.just(new Signal<>(indoor.timestamp, Boolean.FALSE));
            }

            this.indoor = indoor;

            return computeDeviceState(indoor, ambient);


        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Aggregate with {@link #indoor} and pass control to {@link #computeDeviceState(Signal, Signal)}.
     *
     * @return Empty flux if it isn't possible to compute the value, or the target device to execute.
     */
    private Flux<Signal<Boolean, Void>>  computeAmbient(Signal<Double, Void> ambient) {

        ThreadContext.push("computeAmbient");

        try {

            logger.debug("ambient={}", ambient);

            if (ambient.isError()) {

                // Can't do much here other than shut the economizer off, who knows what's going on there
                return Flux.just(new Signal<>(ambient.timestamp, Boolean.FALSE));
            }

            this.ambient = ambient;

            return computeDeviceState(indoor, ambient);

        } finally {
            ThreadContext.pop();
        }
    }

    private Flux<Signal<Boolean, Void>> computeDeviceState(Signal<Double, String> indoor, Signal<Double, Void> ambient) {

        ThreadContext.push("computeDeviceState");

        try {

            logger.debug("indoor={}, ambient={}", indoor, ambient);

            if (indoor == null || ambient == null) {

                // One of the components is not ready yet
                logger.debug("Just staring up? ambient or indoor are null");
                return Flux.empty();
            }

            // The latter one wins
            var timestamp = indoor.timestamp.isAfter(ambient.timestamp) ? indoor.timestamp : ambient.timestamp;

            // VT: FIXME: This will create LOTS of jitter, don't use with units that can't deal with it

            var indoorTemperature = indoor.getValue();
            var ambientTemperature = ambient.getValue();

            logger.debug("indoor={}, ambient={}", indoorTemperature, ambientTemperature);

            Boolean state;
            switch (config.mode) {

                case COOLING:

                    if ((ambient.getValue() < indoorTemperature - config.changeoverDelta) && indoorTemperature > config.targetTemperature) {
                        state = Boolean.TRUE;
                    } else {
                        state = Boolean.FALSE;
                    }
                    break;

                case HEATING:

                    if ((ambient.getValue() > indoorTemperature + config.changeoverDelta) && indoorTemperature < config.targetTemperature) {
                        state = Boolean.TRUE;
                    } else {
                        state = Boolean.FALSE;
                    }
                    break;

                default:

                    logger.error("missing config.mode??? config={}", config);
                    state = Boolean.FALSE;
            }

            logger.debug("state={}", state);

            return Flux.just(new Signal<>(timestamp, state));

        } finally {
            ThreadContext.pop();
        }
    }

    private Signal<ZoneStatus, String> computeZone(Signal<ZoneStatus, String> source) {

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
