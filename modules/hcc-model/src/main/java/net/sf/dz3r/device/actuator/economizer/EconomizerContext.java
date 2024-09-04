package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * A bridge between {@link EconomizerConfig}, {@link AbstractEconomizer economizer}, and {@link net.sf.dz3r.model.Zone}.
 */
public record EconomizerContext(
        EconomizerConfig config,
        Flux<Signal<Double, Void>> ambientFlux,
        HvacDevice device,
        Duration timeout) {

}
