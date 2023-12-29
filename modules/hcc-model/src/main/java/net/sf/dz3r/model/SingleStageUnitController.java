package net.sf.dz3r.model;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import reactor.core.publisher.Flux;

/**
 * Simple single stage unit controller.
 *
 * It's either on, or off.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class SingleStageUnitController extends AbstractUnitController {

    public SingleStageUnitController(String name) {
        super(name);
    }

    /**
     * Transform the demand stream into command stream.
     *
     * @param in Demand stream.
     *
     * @return Command stream.
     */
    @Override
    public Flux<Signal<HvacCommand, Void>> compute(Flux<Signal<UnitControlSignal, Void>> in) {

        return in
                .filter(Signal::isOK)
                .map(s -> {
                    var demand = s.getValue().demand;
                    var output = demand > 0 ? demand : 0;
                    return new Signal<>(s.timestamp, new HvacCommand(null, output, output));
                });
    }
}
