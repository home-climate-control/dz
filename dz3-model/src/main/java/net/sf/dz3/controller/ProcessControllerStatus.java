package net.sf.dz3.controller;

import net.sf.jukebox.datastream.signal.model.DataSample;

/**
 * Process controller status object.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2009
 */
public class ProcessControllerStatus {

    /**
     * Last known setpoint.
     */
    public final double setpoint;
    
    /**
     * Last known error value.
     */
    public final double error;

    /**
     * Last known signal value.
     */
    public final DataSample<Double> signal;

    /**
     * Create an instance.
     * 
     * @param setpoint Last known setpoint.
     * @param error Last known error value.
     * @param signal Last known signal value.
     */
    public ProcessControllerStatus(final double setpoint, final double error, final DataSample<Double> signal) {

	this.setpoint = setpoint;
        this.error = error;
        this.signal = signal;
    }

    /**
     * Get a string representation of the status.
     * 
     * @return Slash separated error and signal values.
     */
    @Override
    public String toString() {

        return Double.toString(error) + "/" + signal;
    }
}
