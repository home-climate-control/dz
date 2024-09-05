package net.sf.dz3r.runtime.config;

import com.homeclimatecontrol.hcc.signal.Signal;
import reactor.core.publisher.Flux;

public record Id2Flux(
        String id,
        Flux<Signal<Double, Void>> flux
) {

}
