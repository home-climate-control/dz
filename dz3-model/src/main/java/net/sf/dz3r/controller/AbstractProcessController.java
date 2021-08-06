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

    protected final Logger logger = LogManager.getLogger();

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
     * Last output signal.
     */
    private Signal<Status<O>> lastOutputSignal = null;

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
     * Get last output signal value.
     *
     * @return Last output signal value, or {@code null} if it is not yet available.
     */
    protected final Signal<Status<O>> getLastOutputSignal() {
        return lastOutputSignal;
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

            // VT: FIXME: Ideally, even the error signal must be passed to wrapCompute() in case it needs to
            // recalculate the state. In practice, this will have to wait.

            // For now, let's throw them a NaN, they better pay attention.
            return new Signal<>(pv.timestamp, new Status(setpoint, null, Double.NaN), pv.status, pv.error);
        }

        if (lastOutputSignal != null && lastOutputSignal.timestamp.isAfter(pv.timestamp)) {
            throw new IllegalArgumentException("Can't go back in time: last sample was @"
                    + lastOutputSignal.timestamp + ", this is @" + pv.timestamp
                    + ", " + (lastOutputSignal.timestamp.toEpochMilli() - pv.timestamp.toEpochMilli()) + "ms difference");
        }

        this.pv = pv;
        lastOutputSignal = wrapCompute(pv);

        return lastOutputSignal;
    }

    protected abstract Signal<Status<O>> wrapCompute(Signal<I> pv);

    /**
     * Acknowledge the configuration change, recalculate and issue control signal if necessary.
     */
    protected abstract void configurationChanged();
}
