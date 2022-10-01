package net.sf.dz3r.device.actuator.economizer.v1;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import reactor.core.publisher.Flux;

/**
 * Economizer implementation.
 *
 * More information: <a href="https://github.com/home-climate-control/dz/wiki/HVAC-Device:-Economizer">HVAC Device: Economizer</a>
 *
 * @param <A> Actuator device address type.
 */
public class Economizer<A extends Comparable<A>> implements SignalProcessor<Double, ZoneStatus, String>, Addressable<String> {

    public final String name;
    public final EconomizerConfig config;

    public final Zone targetZone;

    private final Flux<Signal<Double, Void>> ambientFlux;

    private final Switch<A> targetDevice;

    /**
     * Create an instance.
     *
     * Note that only the {@link #ambientFlux} argument is present, indoor flux is provided to {@link #compute(Flux)}.
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
        this.ambientFlux = ambientFlux;
        this.targetDevice = targetDevice;
    }

    @Override
    public String getAddress() {
        return name;
    }

    /**
     * Compute the virtual zone signal.
     *
     * @param in Indoor temperature flux.
     *
     * @return Flux to send to {@code UnitDirector} instead of the zone flux.
     */
    @Override
    public Flux<Signal<ZoneStatus, String>> compute(Flux<Signal<Double, String>> in) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
