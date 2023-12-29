package net.sf.dz3r.controller.pid;

import net.sf.dz3r.controller.ProcessController;

/**
 * A reactive PID controller abstraction.
 *
 * @param <P> Signal payload type.
 *
 * @see net.sf.dz3r.controller.ProcessController
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface PidController<P> extends ProcessController<Double, Double, P> {

    double getP();

    double getI();

    double getD();

    void setP(double p);
    void setI(double i);
    void setD(double d);

    double getLimit();

    /**
     * Set the integral component saturation limit.
     *
     * @param limit Limit to set.
     */
    void setLimit(double limit);

    /**
     * Reflection of the current status of a PID controller.
     *
     * Note that though the class carries the P, I, D values, it doesn't reflect the PID controller configuration,
     * but state.
     */
    class PidStatus extends Status<Double> {

        /**
         * Proportional value.
         */
        public final double p;

        /**
         * Integral value.
         */
        public final double i;

        /**
         * Derivative value.
         */
        public final double d;

        /**
         * Create an instance.
         *
         * @param template Superclass template
         * @param p Current P value.
         * @param i Current I value.
         * @param d Current D value.
         */
        public PidStatus(Status<Double> template, double p, double i, double d) {

            super(template.setpoint, template.error, template.signal);

            this.p = p;
            this.i = i;
            this.d = d;
        }

        /**
         * Get a string representation of a PID controller status.
         *
         * @return A string representation of a status put together by a superclass,
         * followed by slash delimited P, I, D values.
         */
        @Override
        public final String toString() {
            return super.toString() + ",p=" + p + ",i=" + i + ",d=" + d;
        }
    }
}
