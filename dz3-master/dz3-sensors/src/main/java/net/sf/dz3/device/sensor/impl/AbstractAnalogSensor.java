package net.sf.dz3.device.sensor.impl;

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.jukebox.conf.ConfigurableProperty;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.service.ActiveService;

/**
 * An abstract analog sensor.
 * 
 * <p>
 * 
 * Supports the common configuration and listener notification features.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public abstract class AbstractAnalogSensor extends ActiveService implements AnalogSensor {

    /**
     * Current signal value.
     * 
     * <p>
     * 
     * This value has to be updated by {@link #execute()} and used by {@link #getSignal()} in order to provide a fast
     * response.
     * 
     * <p>
     * 
     * If the value is <code>null</code>, it means that no signal readings are available yet.
     * 
     * VT: FIXME: Visibility of this member is set to protected to allow reverse update mechanism - see
     * <code>AbstractDeviceFactory$SensorProxy</code>.
     */
    protected DataSample<Double> currentSignal = null;

    /**
     * Logger data broadcaster.
     */
    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();

    /**
     * The poll interval.
     * 
     * <p>
     * 
     * Sleep this many milliseconds between measuring the temperature and possibly reporting it.
     * 
     * <p>
     * 
     * 5000ms is a reasonable default for most applications.
     */
    private long pollIntervalMillis = 5000;

    /**
     * Hardware address of the sensor on the remote end to watch for.
     */
    private final String address;

    public AbstractAnalogSensor(String address, int pollIntervalMillis) {

        // Sensor address will never change, we will only accept it in the constructor.
        this.address = address;

        setPollInterval(pollIntervalMillis);
    }

    @JmxAttribute(description = "Poll interval, milliseconds")
    public final long getPollInterval() {
        return pollIntervalMillis;
    }

    @ConfigurableProperty(propertyName = "pollIntervalMillis", description = "Poll interval, milliseconds")
    public final void setPollInterval(long pollIntervalMillis) {

        if (pollIntervalMillis < 0) {
            throw new IllegalArgumentException("Poll interval must be positive");
        }

        if (pollIntervalMillis < 1000) {
            logger.warn("Unreasonably short poll interval (" + pollIntervalMillis + ")");
        }

        this.pollIntervalMillis = pollIntervalMillis;
    }

    /**
     * {@inheritDoc}
     */
    public final String getAddress() {

        return address;
    }

    /**
     * {@inheritDoc}
     */
    public final DataSample<Double> getSignal() {

        return currentSignal;
    }

    @Override
    protected void execute() {

        if (getPollInterval() < 0) {

            throw new IllegalStateException("Negative poll interval (" + getPollInterval() + ")???");
        }

        ThreadContext.push("execute@" + getAddress());

        try {

            while (isEnabled()) {

                currentSignal = getSensorSignal();

                logger.debug("Current signal: " + currentSignal);

                // VT: NOTE: We will notify the listeners even if the
                // signal hasn't changed - their processing logic may
                // suck and just get stuck if they don't get frequent
                // notifications

                broadcast(currentSignal);

                Thread.sleep(pollIntervalMillis);
            }

        } catch (Throwable t) {
            logger.fatal("Unexpected problem, shutting down:", t);
        } finally {
            ThreadContext.pop();
            ThreadContext.clearStack();
        }
    }

    protected final void broadcast(DataSample<Double> signal) {

        dataBroadcaster.broadcast(signal);
    }

    /**
     * Get the actual hardware device signal.
     * 
     * @return The sensor signal.
     * 
     * @exception IOException
     *                if there was a problem communicating with the hardware sensor.
     */
    public abstract DataSample<Double> getSensorSignal() throws IOException;

    public void addConsumer(DataSink<Double> consumer) {
        dataBroadcaster.addConsumer(consumer);
    }

    public void removeConsumer(DataSink<Double> consumer) {
        dataBroadcaster.removeConsumer(consumer);
    }
}
