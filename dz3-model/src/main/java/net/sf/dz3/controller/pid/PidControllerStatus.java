package net.sf.dz3.controller.pid;

import net.sf.dz3.controller.ProcessControllerStatus;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2009
 */
public class PidControllerStatus extends ProcessControllerStatus {

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
     * @param setpoint Current setpoint.
     * @param error Current error value.
     * @param signal Current signal value.
     * @param p Current P value.
     * @param i Current I value.
     * @param d Current D value.
     */
    public PidControllerStatus(double setpoint, double error, DataSample<Double> signal, double p, double i, double d) {

        super(setpoint, error, signal);

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

        return super.toString() + "/" + p + "/" + i + "/" + d;
    }
}
