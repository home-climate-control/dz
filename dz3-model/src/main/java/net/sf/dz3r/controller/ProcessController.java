package net.sf.dz3r.controller;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

/**
 * A reactive process controller abstraction.
 *
 * The controller is expected to produce an output based on the value of the
 * process variable and event time. The exact details are left to the
 * implementation.
 *
 * @param <A> Signal address type.
 *
 * @see net.sf.dz3.controller.ProcessController
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public interface ProcessController<A extends Comparable<A>> {

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
    Signal<A, Double> getProcessVariable();

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
     * @return Output signal flux.
     */
    Flux<Signal<A, Double>> compute(Flux<Signal<A, Double>> pv);

    @JmxAttribute(description = "Status")
    Status<A> getStatus();

    public static class Status<A extends Comparable<A>> {

        public final double setpoint;
        public final double error;
        public final Signal<A, Double> signal;

        public Status(double setpoint, double error, Signal<A, Double> signal) {
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
