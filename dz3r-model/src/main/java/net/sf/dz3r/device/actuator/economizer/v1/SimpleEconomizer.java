package net.sf.dz3r.device.actuator.economizer.v1;

import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.economizer.AbstractEconomizer;
import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;

/**
 * Simple economizer implementation with no jitter control (rather a prototype).
 *
 * More information: <a href="https://github.com/home-climate-control/dz/wiki/HVAC-Device:-Economizer">HVAC Device: SimpleEconomizer</a>
 *
 * @param <A> Actuator device address type.
 */
public class SimpleEconomizer<A extends Comparable<A>> extends AbstractEconomizer {

    /**
     * Create an instance.
     *
     * Note that only the {@code ambientFlux} argument is present, indoor flux is provided to {@link #compute(Flux)}.
     *
     * @param ambientFlux Flux from the ambient temperature sensor.
     * @param device HVAC device acting as the economizer.
     */
    public SimpleEconomizer(
            String name,
            EconomizerSettings settings,
            Flux<Signal<Double, Void>> ambientFlux,
            HvacDevice device) {

        super(null, name, settings, device);

        initFluxes(ambientFlux);
    }

    @Override
    protected Flux<Signal<Boolean, ProcessController.Status<Double>>> computeDeviceState(Flux<Signal<Double, Void>> signal) {

        return signal
                .map(this::computeState);
    }

    private Signal<Boolean, ProcessController.Status<Double>> computeState(Signal<Double, Void> signal) {

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
