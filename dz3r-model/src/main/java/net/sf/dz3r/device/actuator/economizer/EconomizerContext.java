package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

/**
 * A bridge between {@link EconomizerSettings}, {@link AbstractEconomizer economizer}, and {@link net.sf.dz3r.model.Zone}.
 */
public class EconomizerContext {

    public final EconomizerSettings settings;
    public final Flux<Signal<Double, Void>> ambientFlux;
    public final HvacDevice device;

    public EconomizerContext(EconomizerSettings settings, Flux<Signal<Double, Void>> ambientFlux, HvacDevice device) {
        this.settings = settings;
        this.ambientFlux = ambientFlux;
        this.device = device;
    }
}
