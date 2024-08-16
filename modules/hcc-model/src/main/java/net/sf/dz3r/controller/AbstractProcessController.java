package net.sf.dz3r.controller;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Optional;

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
    private Double setpoint;

    private final Sinks.Many<Optional<Double>> setpointSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Flux<Optional<Double>> setpointFlux = setpointSink.asFlux();

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
    protected AbstractProcessController(String jmxName, Double setpoint) {
        this.jmxName = jmxName;

        setpointFlux.subscribe(this::setSetpoint);

        setSetpoint(setpoint);
    }

    private void setSetpoint(Optional<Double> setpoint) { // NOSONAR This use of Optional as an argument has been considered and accepted
        logger.trace("{}: setSetpoint={}", jmxName, setpoint);
        this.setpoint = setpoint.orElse(null);
    }

    @Override
    public void setSetpoint(Double setpoint) {
        setpointSink.tryEmitNext(Optional.ofNullable(setpoint));
    }

    @Override
    public Double getSetpoint() {
        return setpoint;
    }

    @Override
    public Signal<I, P> getProcessVariable() {
        return pv;
    }

    @Override
    public final synchronized Double getError() {

        if (setpoint == null || pv == null) {
            return null;
        }

        return getError(pv, setpoint);
    }

    protected abstract double getError(Signal<I, P> pv, Double setpoint);

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
                        Flux.just(Optional.ofNullable(setpoint)),
                        setpointFlux
                ),
                pv.doOnComplete(setpointSink::tryEmitComplete), // or it will hang forever
                this::compute);
    }

    /**
     * Compute the controller signal.
     *
     * Using {@code Optional} for arguments is an antipattern, but an unavoidable one -
     * the setpoint is supplied by a {@code Flux} which doesn't support null values, so some kind of wrapper
     * would've been necessary anyway, so why not use whatever is intended for this specifically.
     *
     * @param setpoint Setpoint wrapped into {@code Optional}.
     * @param pv Process variable.
     *
     * @return Computed signal.
     */
    private Signal<Status<O>, P> compute(Optional<Double> setpoint, Signal<I, P> pv) { // NOSONAR This use of Optional as an argument has been considered and accepted

        // VT: NOTE: https://github.com/home-climate-control/dz/issues/321 - no more "magic numbers"

        if (lastOutputSignal != null && lastOutputSignal.timestamp.isAfter(pv.timestamp)) {
            logger.warn("Can't go back in time: last sample was @{}, this is @{}, {}ms difference: {}",
                    lastOutputSignal.timestamp,
                    pv.timestamp,
                    Duration.between(lastOutputSignal.timestamp, pv.timestamp).toMillis(),
                    pv);
        }

        this.pv = pv;
        lastOutputSignal = wrapCompute(setpoint.orElse(null), pv);

        return lastOutputSignal;
    }

    protected abstract Signal<Status<O>, P> wrapCompute(Double setpoint, Signal<I, P> pv);

    /**
     * Acknowledge the configuration change, recalculate and issue control signal if necessary.
     */
    protected abstract void configurationChanged();
}
