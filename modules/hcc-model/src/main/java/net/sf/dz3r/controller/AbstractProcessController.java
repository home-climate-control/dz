package net.sf.dz3r.controller;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;

/**
 * Base class for reactive process controllers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class AbstractProcessController<I, O, P> implements ProcessController<I, O, P> {

    protected final Logger logger = LogManager.getLogger();

    public final String jmxName;

    /**
     * The process setpoint.
     */
    private double setpoint;

    private final Flux<Double> setpointFlux;
    private FluxSink<Double> setpointSink;

    /**
     * The current process variable value.
     */
    private Signal<I, P> pv;

    /**
     * Last output signal.
     */
    private Signal<Status<O>, P> lastOutputSignal = null;

    /**
     * Create an instance.
     *
     * @param jmxName This controller's JMX name.
     * @param setpoint Initial setpoint.
     */
    protected AbstractProcessController(String jmxName, double setpoint) {
        this.jmxName = jmxName;

        setpointFlux = Flux.create(this::connectSetpoint);
        setpointFlux.subscribe(s -> this.setpoint = s);

        setSetpoint(setpoint);
    }

    private void connectSetpoint(FluxSink<Double> setpointSink) {
        this.setpointSink = setpointSink;
    }

    @Override
    public void setSetpoint(double setpoint) {
        setpointSink.next(setpoint);
    }

    @Override
    public double getSetpoint() {
        return setpoint;
    }

    @Override
    public Signal<I, P> getProcessVariable() {
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

    protected abstract double getError(Signal<I, P> pv, double setpoint);

    /**
     * Get last output signal value.
     *
     * @return Last output signal value, or {@code null} if it is not yet available.
     */
    protected final Signal<Status<O>, P> getLastOutputSignal() {
        return lastOutputSignal;
    }

    @Override
    public final Flux<Signal<Status<O>, P>> compute(Flux<Signal<I, P>> pv) {

        // Whatever is in setpointFlux already, will not get replayed, and pv signals will get lost.
        // Need to re-inject it.
        return Flux.combineLatest(
                Flux.concat(
                        Flux.just(setpoint),
                        setpointFlux
                ),
                pv.doOnComplete(() -> setpointSink.complete()), // or it will hang forever
                this::compute);
    }

    private Signal<Status<O>, P> compute(Double setpoint, Signal<I, P> pv) {

        if (pv.isError()) {

            // VT: FIXME: Ideally, even the error signal must be passed to wrapCompute() in case it needs to
            // recalculate the state. In practice, this will have to wait.

            // For now, let's throw them a NaN, they better pay attention.
            return new Signal<>(pv.timestamp, new Status(setpoint, null, Double.NaN), pv.payload, pv.status, pv.error);
        }

        if (lastOutputSignal != null && lastOutputSignal.timestamp.isAfter(pv.timestamp)) {
            logger.warn("Can't go back in time: last sample was @{}, this is @{}, {}ms difference: {}",
                    lastOutputSignal.timestamp,
                    pv.timestamp,
                    Duration.between(lastOutputSignal.timestamp, pv.timestamp).toMillis(),
                    pv);
        }

        this.pv = pv;
        lastOutputSignal = wrapCompute(setpoint, pv);

        return lastOutputSignal;
    }

    protected abstract Signal<Status<O>, P> wrapCompute(Double setpoint, Signal<I, P> pv);

    /**
     * Acknowledge the configuration change, recalculate and issue control signal if necessary.
     */
    protected abstract void configurationChanged();
}
