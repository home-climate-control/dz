package net.sf.dz3r.model;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.UnitControlSignal;
import reactor.core.publisher.Flux;

/**
 * Simple single stage unit controller.
 *
 * It's either on, or off.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SingleStageUnitController extends AbstractUnitController {

    protected SingleStageUnitController(String name) {
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
                    double value = s.getValue().demand > 0 ? 1 : 0;
                    return new Signal<>(s.timestamp, new HvacCommand(null, value, value));
                });
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "HVAC Unit Controller (single stage)",
                getAddress(),
                "Issues commands to single stage HVAC Unit Driver");
    }
}
