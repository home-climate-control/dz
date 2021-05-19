package net.sf.dz3.controller;

import com.homeclimatecontrol.jukebox.conf.ConfigurableProperty;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;

/**
 * A process controller abstraction.
 *
 * <p>
 *
 * The controller is expected to produce an output based on the value of the
 * process variable and measurement time. The exact details are left to the
 * implementation.
 *
 * <p>
 *
 * VT: FIXME: Think about how to make this work nicely with {@link DataSource} and {@link DataSink} entities.
 * Controllers normally have one source of input data. DZ3 implementation, so far, doesn't have
 * consumers that feed from more than one process controller that need to be distinguished other than by signature.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
 */
public interface ProcessController extends DataSink<Double>, DataSource<ProcessControllerStatus> {

    /**
     * Set the setpoint.
     *
     * @param setpoint Setpoint to set.
     */
    @ConfigurableProperty(
        propertyName = "setpoint",
        description = "Desired Setpoint"
    )
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
    DataSample<Double> getProcessVariable();

	/**
     * Get the current value of the error.
     *
     * @return Current error value.
     */
    @JmxAttribute(description = "Error")
    double getError();

    /**
     * Compute the controller output given the current value of the process
     * variable at a given time.
     *
     * @param pv Process variable. Can be {@link DataSample#isError() an error}.
     *
     * @return A computed controller output (corrective action), with the same timestamp as
     * the input value.
     *
     * @see #consume(DataSample)
     */
    DataSample<Double> compute(DataSample<Double> pv);

    /**
     * The asynchronous counterpart to {@link #compute(DataSample)}.
     *
     * {@link #compute(DataSample)} will eventually result in a state change notification broadcast
     * (at least per current implementation), so the net result of {@link #compute(DataSample)} and
     * {@link #consume(DataSample)} invocations is the same - except that {@link #consume(DataSample)}
     * will not return a computed value.
     */
    @Override
    void consume(DataSample<Double> sample);

    /**
     * Get the extended process controller status.
     *
     * <p>
     *
     * Since all the classes implementing this interface will have different
     * way of handling the status, there has to be a way to get the
     * controller status as a single object.
     *
     * <p>
     *
     * It would be reasonable to expect that the particular object returned
     * by the controller will implement {@code toString()} in a way
     * that allows to comprehend the status without getting into details.
     *
     * <p>
     *
     * This method ideally shouldn't throw any exceptions.
     *
     * @return The object representing a full process controller status.
     */
    @JmxAttribute(description = "Status")
    ProcessControllerStatus getStatus();
}
