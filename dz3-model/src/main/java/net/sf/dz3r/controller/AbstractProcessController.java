package net.sf.dz3r.controller;

import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

/**
 * Base class for reactive process controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractProcessController<I, O> implements ProcessController<I, O>, JmxAware {

    protected final Logger logger = LogManager.getLogger(getClass());

    public final String jmxName;

    /**
     * The process setpoint.
     */
    private double setpoint;

    /**
     * The current process variable value.
     */
    private Signal<I> pv;

    /**
     * Last known signal.
     */
    private Signal<Status<O>> lastKnownSignal = null;

    /**
     * Create an instance.
     *
     * @param jmxName This controller's JMX name.
     * @param setpoint Initial setpoint.
     */
    protected AbstractProcessController(String jmxName, double setpoint) {
        this.jmxName = jmxName;
        this.setpoint = setpoint;
    }

    @Override
    public void setSetpoint(double setpoint) {

        this.setpoint = setpoint;

        // May need to recalculate the status and emit the next computed value
        configurationChanged();
    }

    @Override
    public double getSetpoint() {
        return setpoint;
    }

    @Override
    public Signal<I> getProcessVariable() {
        return pv;
    }

    @Override
    public final synchronized double getError() {

        if (pv == null) {
            // No sample, no error
            return 0;
        }

        return getError(pv, setpoint);
    }

    protected abstract double getError(Signal<I> pv, double setpoint);

    /**
     * Get last known signal value.
     *
     * @return Last known signal value, or {@code null} if it is not yet available.
     */
    protected final Signal<Status<O>> getLastKnownSignal() {
        return lastKnownSignal;
    }

    @Override
    public final Flux<Signal<Status<O>>> compute(Flux<Signal<I>> pv) {
        return pv.map(this::doCompute);
    }

    private Signal<Status<O>> doCompute(Signal<I> pv) {

        if (pv == null) {
            throw new IllegalArgumentException("pv can't be null");
        }

        if (pv.isError()) {
            // VT: NOTE: Unlike previous incarnation, this one does let the errors through, let the implementation sort it out
            // it is expected that wrapCompute() knows to do with an error sample.
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

    protected abstract Signal<Status<O>> wrapCompute(Signal<I> pv);

    /**
     * Acknowledge the configuration change, recalculate and issue control signal if necessary.
     */
    protected abstract void configurationChanged();
}
