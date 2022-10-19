package net.sf.dz3r.device.actuator.economizer.v1;

import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.device.actuator.economizer.AbstractEconomizer;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;

/**
 * Simple economizer implementation with no jitter control (rather a prototype).
 *
 * More information: <a href="https://github.com/home-climate-control/dz/wiki/HVAC-Device:-Economizer">HVAC Device: Economizer</a>
 *
 * @param <A> Actuator device address type.
 */
public class Economizer<A extends Comparable<A>> extends AbstractEconomizer<A> {

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

        super(name, config, targetZone, ambientFlux, targetDevice);

        initFluxes(ambientFlux);
    }

    @Override
    protected Flux<Signal<Boolean, Void>> computeDeviceState(Flux<Signal<Double, Void>> signal) {

        return signal
                .map(this::computeState);
    }

    private Signal<Boolean, Void> computeState(Signal<Double, Void> signal) {

        ThreadContext.push("computeDeviceState");

        try {

            // VT: NOTE: This will create LOTS of jitter, don't use with units that can't deal with it

            Boolean state = signal.getValue() > 0 ? Boolean.TRUE : Boolean.FALSE;

            logger.debug("state={}", state);

            return new Signal<>(signal.timestamp, state);

        } finally {
            ThreadContext.pop();
        }
    }
}
