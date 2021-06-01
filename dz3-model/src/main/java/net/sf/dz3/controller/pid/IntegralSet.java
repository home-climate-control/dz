package net.sf.dz3.controller.pid;

import net.sf.dz3.controller.DataSet;

/**
 * Data set supporting the integration calculation.
 * <p>
 * The {@link DataSet#record record()} method from {@link DataSet DataSet} class
 * is used, however, make sure you record the right values. If this class is
 * used for the {@link PID_Controller}, it must be fed with controller error,
 * and anti-windup action must be programmed outside of this class.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2015
 */
public interface IntegralSet {
    
    /**
     * @see DataSet#record(long, Object)
     */
    void record(long millis, Double value);

    /**
     * Get the integral starting with the first data element available and
     * ending with the last data element available.
     * <p>
     * Integration time must have been taken care of by {@link DataSet#expire
     * expiration}.
     *
     * @return An integral value.
     */
    double getIntegral();
}
