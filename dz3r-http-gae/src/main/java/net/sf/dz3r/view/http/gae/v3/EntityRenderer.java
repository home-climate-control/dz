package net.sf.dz3r.view.http.gae.v3;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.http.gae.v3.wire.ZoneSnapshot;
import reactor.core.publisher.Flux;

abstract class EntityRenderer<I, P> {
    public abstract Flux<ZoneSnapshot> compute(Flux<Signal<I, P>> in);
}
