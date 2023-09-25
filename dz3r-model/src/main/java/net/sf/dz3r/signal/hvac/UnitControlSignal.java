package net.sf.dz3r.signal.hvac;

import reactor.core.publisher.Flux;

/**
 * Signal to control the unit.
 *
 * @see net.sf.dz3r.model.UnitController
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class UnitControlSignal {

    /**
     * Demand to pass down to {@link net.sf.dz3r.model.UnitController#compute(Flux)}.
     */
    public final Double demand;

    /**
     * Fan speed to pass down to {@link net.sf.dz3r.model.UnitController#compute(Flux)}.
     *
     * Value of {@code null} indicates a command to leave the value as is.
     */
    public final Double fanSpeed;


    public UnitControlSignal(double demand, Double fanSpeed) {
        this.demand = demand;
        this.fanSpeed = fanSpeed;
    }

    @Override
    public String toString() {
        return "{demand=" + demand + ", fanSpeed=" + fanSpeed + "}";
    }
}
