package net.sf.dz3r.controller;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

/**
 * Base class for reactive process controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractProcessController<A extends Comparable<A>> implements ProcessController<A> {

    protected final Logger logger = LogManager.getLogger(getClass());

    /**
     * The process setpoint.
     */
    private double setpoint;

    /**
     * The current process variable value.
     */
    private Signal<A, Double> pv;

    /**
     * Last known signal.
     */
    private Signal<A, Double> lastKnownSignal = null;

    /**
     * Create an instance.
     *
     * @param setpoint Initial setpoint.
     */
    protected AbstractProcessController(double setpoint) {
        this.setpoint = setpoint;
    }

    @Override
    public void setSetpoint(double setpoint) {

        this.setpoint = setpoint;

        // VT: FIXME: Need to recalculate the status and issue the next computed value
    }

    @Override
    public double getSetpoint() {
        return setpoint;
    }

    @Override
    public Signal<A, Double> getProcessVariable() {
        return pv;
    }

    @Override
    public Signal<A, Double> getError() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public final Flux<Signal<A, Double>> compute(Flux<Signal<A, Double>> pv) {
        return pv.map(this::doCompute);
    }

    private Signal<A, Double> doCompute(Signal<A, Double> pv) {

        if (pv == null) {
            throw new IllegalArgumentException("pv can't be null");
        }

        if (pv.isError()) {
            // VT: NOTE: Unlike previous incarnation, this one does let the errors through, let the implementation sort it out
        }

        if (lastKnownSignal != null && lastKnownSignal.timestamp.isAfter(pv.timestamp)) {
            throw new IllegalArgumentException("Can't go back in time: last sample was @"
                    + lastKnownSignal.timestamp + ", this is @" + pv.timestamp
                    + ", " + (lastKnownSignal.timestamp.toEpochMilli() - pv.timestamp.toEpochMilli()) + "ms difference");
        }

        this.pv = pv;
        this.lastKnownSignal = wrapCompute(pv);

        return lastKnownSignal;
    }

    protected abstract Signal<A, Double> wrapCompute(Signal<A, Double> pv);

    @Override
    public Status<A> getStatus() {
        return new Status<>(setpoint, lastKnownSignal, getError());
    }
}
