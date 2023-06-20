package net.sf.dz3r.controller;

import net.sf.dz3r.jmx.JmxAttribute;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import reactor.core.publisher.Flux;

/**
 * A reactive process controller abstraction.
 *
 * The controller is expected to produce an output based on the value of the
 * process variable and event time. The exact details are left to the
 * implementation.
 *
 * @param <I> Process variable type.
 * @param <O> Signal type.
 * @param <P> Signal payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public interface ProcessController<I, O, P> extends SignalProcessor<I, ProcessController.Status<O>, P> {

    /**
     * Set the setpoint.
     *
     * @param setpoint Setpoint to set.
     */
    void setSetpoint(double setpoint);

    /**
     * Get the setpoint.
     *
     * @return Current setpoint value.
     */
    @JmxAttribute(description = "Setpoint")
    double getSetpoint();

    /**
     * Get the process variable.
     *
     * @return The process variable.
     */
    @JmxAttribute(description = "Process Variable")
    Signal<I, P> getProcessVariable();

    /**
     * Get the current value of the error.
     *
     * @return Current error value.
     */
    @JmxAttribute(description = "Error")
    double getError();

    /**
     * Compute the output signal.
     *
     * @param pv Process variable flux.
     *
     * @return Output signal flux. The end of this flux indicates the need for the subscriber to shut down.
     */
    @Override
    Flux<Signal<Status<O>, P>> compute(Flux<Signal<I, P>> pv);

    class Status<T> {

        public final double setpoint;
        public final Double error;
        public final T signal;

        public Status(double setpoint, Double error, T signal) {
            this.setpoint = setpoint;
            this.error = error;
            this.signal = signal;
        }

        @Override
        public String toString() {
            return "setpoint=" + setpoint + ",error=" + error + ",signal=" + signal;
        }
    }
}
