package net.sf.dz3r.model;

import net.sf.dz3r.controller.SignalProcessor;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.ZoneStatus;
import reactor.core.publisher.Flux;

/**
 * Accepts signals from {@link Zone zones} and issues signals to Unit and Damper Controller.
 *
 * VT: FIXME: Augment the description with links once those entities are ported to reactive streams.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZoneController implements SignalProcessor<ZoneStatus, Double, String> {

    @Override
    public Flux<Signal<Double, String>> compute(Flux<Signal<ZoneStatus, String>> in) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
