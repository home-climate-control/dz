package net.sf.dz3r.model;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.UnitControlSignal;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Multistage unit controller.
 *
 * Makes a decision about which stage to go to based on demand.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class MultistageUnitController extends AbstractUnitController {

    /**
     * @see #setStages(List)
     */
    private final List<Double> stages = new ArrayList<>();

    protected MultistageUnitController(String name, List<Double> stages) {
        super(name);
        setStages(stages);
    }

    /**
     * Set the stage divisions.
     *
     * @param stages An array of values determining the stage change point. All values must be positive.
     *
     * @return Sorted immutable clone of the {@link #stages}. The order may differ from the order in the given collection.
     */
    public synchronized List<Double> setStages(List<Double> stages) {

        if (stages.isEmpty()) {
            throw new IllegalArgumentException("Empty stage change point list doesn't make sense");
        }

        for (var stage : stages) {
            if (stage <= 0) {
                throw new IllegalArgumentException("Stage change point must be positive: " + stage);
            }
        }

        this.stages.clear();
        this.stages.addAll(stages.stream().sorted().collect(Collectors.toList()));

        return Collections.unmodifiableList(this.stages);
    }

    @Override
    public Flux<Signal<HvacCommand, Void>> compute(Flux<Signal<UnitControlSignal, Void>> in) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "HVAC Unit Controller (multistage)",
                getAddress(),
                "Issues commands to multistage HVAC Unit Driver");
    }
}
