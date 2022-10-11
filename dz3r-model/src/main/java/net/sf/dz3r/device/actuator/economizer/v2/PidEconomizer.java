package net.sf.dz3r.device.actuator.economizer.v2;

import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.device.actuator.economizer.AbstractEconomizer;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

/**
 * Economizer implementation with PID jitter control.
 *
 * More information: <a href="https://github.com/home-climate-control/dz/wiki/HVAC-Device:-Economizer">HVAC Device: Economizer</a>
 *
 * @param <A> Actuator device address type.
 */
public class PidEconomizer<A extends Comparable<A>> extends AbstractEconomizer<A> {

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
    protected PidEconomizer(
            String name,
            EconomizerConfig config,
            Zone targetZone,
            Flux<Signal<Double, Void>> ambientFlux,
            Switch<A> targetDevice) {

        super(name, config, targetZone, ambientFlux, targetDevice);
    }
    @Override
    protected Flux<Signal<Boolean, Void>> computeDeviceState(Flux<Signal<Double, Void>> signal) {

        throw new UnsupportedOperationException("Not Implemented");
    }
}
