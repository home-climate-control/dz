package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

/**
 * A bridge between {@link EconomizerSettings}, {@link AbstractEconomizer economizer}, and {@link net.sf.dz3r.model.Zone}.
 */
public class EconomizerContext<A extends Comparable<A>> {

    public final EconomizerSettings settings;
    public final Flux<Signal<Double, Void>> ambientFlux;
    public final Switch<A> targetDevice;

    public EconomizerContext(EconomizerSettings settings, Flux<Signal<Double, Void>> ambientFlux, Switch<A> targetDevice) {
        this.settings = settings;
        this.ambientFlux = ambientFlux;
        this.targetDevice = targetDevice;
    }
}
