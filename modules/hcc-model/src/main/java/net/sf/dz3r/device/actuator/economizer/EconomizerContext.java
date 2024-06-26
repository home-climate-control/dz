package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * A bridge between {@link EconomizerConfig}, {@link AbstractEconomizer economizer}, and {@link net.sf.dz3r.model.Zone}.
 */
public class EconomizerContext {

    public final EconomizerConfig config;
    public final Flux<Signal<Double, Void>> ambientFlux;
    public final HvacDevice device;
    public final Duration timeout;

    public EconomizerContext(EconomizerConfig config, Flux<Signal<Double, Void>> ambientFlux, HvacDevice device, Duration timeout) {
        this.config = config;
        this.ambientFlux = ambientFlux;
        this.device = device;
        this.timeout = timeout;
    }
}
