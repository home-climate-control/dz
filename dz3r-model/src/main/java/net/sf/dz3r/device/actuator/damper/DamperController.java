package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import reactor.core.publisher.Flux;

public interface DamperController extends AutoCloseable {

    /**
     * Compute the damper status out of unit and zones status.
     *
     * @param unitFlux Output of {@link net.sf.dz3r.model.ZoneController#compute(Flux)}. Only {@link UnitControlSignal#fanSpeed}
     *                 is considered, {@link UnitControlSignal#demand} is ignored (corollary: simpler implementations must
     *                 enforce {@code fanSpeed} to be non-zero if {@code demand} is).
     * @param zoneFlux Aggregate output of {@link net.sf.dz3r.model.Zone#compute(Flux)}, with the payload being treated as a zone name.
     *
     * @return Flux of fluxes of damper positions. Every incoming signal will cause a flux to be emitted.
     */
    Flux<Flux<Signal<Damper<?>, Double>>> compute(Flux<Signal<UnitControlSignal, Void>> unitFlux, Flux<Signal<ZoneStatus, String>> zoneFlux);
}
