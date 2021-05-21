package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import com.homeclimatecontrol.jukebox.logger.LogAware;
import com.homeclimatecontrol.jukebox.util.Interval;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.UnitSignal;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.util.digest.MessageDigestCache;
import org.apache.logging.log4j.ThreadContext;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2019
 */
public class UnitModel extends LogAware implements Unit {

    /**
     * The unit name.
     */
    private final String name;

    /**
     * Instrumentation signature.
     */
    private final String signature;

    private DataBroadcaster<UnitSignal> dataBroadcaster = new DataBroadcaster<UnitSignal>();

    /**
     * Last known state.
     */
    private DataSample<UnitSignal> state;

    /**
     * Time when the unit was started, in milliseconds.
     * {@code null} value indicates it is not currently running.
     */
    private Long lastStarted = null;

    /**
     * Minimum runtime allowed, in milliseconds.
     *
     * No minimum runtime restriction if the value is 0.
     */
    private long minRuntimeMillis = 0;

    /**
     * Create a named instance with nothing connected to it.
     *
     * @param name Unit name.
     */
    public UnitModel(String name) {

        this(name, null, 0);
    }

    /**
     * Create a named instance listening to the given zone controller with no minimum runtime restriction.
     *
     * @param name Unit name.
     * @param zoneController Zone controller to listen to.
     */
    public UnitModel(String name, ZoneController zoneController) {

        this(name, zoneController, 0);
    }

    /**
     * Create a named instance listening to the given zone controller.
     *
     * @param name Unit name.
     * @param zoneController Zone controller to listen to.
     * @param minRuntimeMillis Minimum runtime allowed, in milliseconds.
     */
    public UnitModel(String name, ZoneController zoneController, long minRuntimeMillis) {

        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("name can't be null or empty");
        }

        this.name = name;
        signature = MessageDigestCache.getMD5(name).substring(0, 19);

        setMinRuntime(minRuntimeMillis);

        if (zoneController != null) {
            zoneController.addConsumer(this);
        }
    }

    @Override
    public String getName() {

        if ( name == null ) {

            throw new IllegalStateException("Not Initialized");
        }

        return name;
    }

    @Override
    public int compareTo(Unit other) {

	if (other == null) {
	    throw new IllegalArgumentException("other can't be null");
	}

	return getName().compareTo(other.getName());
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("Unit(").append(name).append(", ");
        sb.append(state == null ? "<not initialized>" : getSignal());
        sb.append(")");

        return sb.toString();
    }

    public UnitSignal getSignal() {
        return state.sample;
    }

    @JmxAttribute(description="Demand")
    public double getDemand() {
        return state != null ? state.sample.demand : 0;
    }


    @JmxAttribute(description="Running")
    public boolean getRunning() {
        return state != null ? state.sample.running : false;
    }

    @JmxAttribute(description="Uptime as string")
    public String getUptimeAsString() {

        if (lastStarted == null) {
            return "off";
        }

        // VT: NOTE: The only reason currentTimeMillis() is allowed here is because
        // this method is for human consumption via JMX Console

        return Interval.toTimeInterval(System.currentTimeMillis() - lastStarted);
    }

    /**
     * Compute the operating state based on demand.
     *
     * @param signal Zone controller signal to make a decision upon.
     */
    @Override
    public synchronized void consume(DataSample<Double> signal) {

        ThreadContext.push("consume");

        try {

            check(signal);

            logger.debug("demand (old, new): (" + (state == null ? Double.NaN : state.sample.demand) + ", " + signal.sample + ")");

            // VT: FIXME: uptime

            if (signal.sample > 0 && !isRunning()) {

                logger.info("Turning ON");
                setRunning(true, signal.timestamp);

            } else if (signal.sample == 0 && isRunning()) {

                logger.info("Turning OFF");
                setRunning(false, signal.timestamp);

            } else {

                logger.debug("no change");
            }

        } finally {

            setDemand(signal.sample, signal.timestamp);
            ThreadContext.pop();
        }
    }

    /**
     * Make sure the signal given to {@link #consume(DataSample)} is sane.
     *
     * @param signal Signal to check.
     */
    private void check(DataSample<Double> signal) {

        ThreadContext.push("check");

        try {

            if (signal == null) {
                throw new IllegalArgumentException("signal can't be null");
            }

            if (signal.isError()) {

                logger.error("Should not have propagated all the way here", signal.error);
                throw new IllegalArgumentException("Error signal should have been handled by zone controller");
            }

            if (signal.sample < 0.0) {

                throw new IllegalArgumentException("Signal must be non-negative, received " + signal.sample);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    public final boolean isRunning() {
        return state == null ? false : state.sample.running;
    }

    /**
     * Broadcast the state change.
     */
    private void stateChanged() {

        ThreadContext.push("stateChanged");

        try {

            if (state == null) {

                logger.warn("state is null, invoked from constructor?");
                return;
            }

            dataBroadcaster.broadcast(state);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public void addConsumer(DataSink<UnitSignal> consumer) {

        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<UnitSignal> consumer) {

        dataBroadcaster.removeConsumer(consumer);
    }

    /**
     * Switch the unit on or off.
     *
     * This method should not be called more often than it is necessary.
     * Nevertheless, it must be idempotent to increase fault tolerance.
     *
     * @param running Desired running mode.
     */
    private synchronized void setRunning(boolean running, long timestamp) {

        long uptime = updateUptime(running, timestamp);

        state = new DataSample<UnitSignal>(timestamp,
                name,
                signature,
                new UnitSignal(state == null ? 0.0 : state.sample.demand, running, uptime), null);

        stateChanged();
    }

    private synchronized void setDemand(double demand, long timestamp) {

        long uptime = updateUptime(demand > 0, timestamp);

        state = new DataSample<UnitSignal>(timestamp,
                name,
                signature,
                new UnitSignal(demand, state == null ? false : state.sample.running, uptime), null);

        stateChanged();
    }

    private synchronized long updateUptime(boolean running, long timestamp) {

        if (!running) {
            lastStarted = null;
            return 0;
        }

        if (lastStarted == null) {
            lastStarted = timestamp;
            return 0;
        }

        if (timestamp < lastStarted) {
            throw new IllegalStateException("timestamp < lastStarted (" + timestamp + " < " + lastStarted + ")");
        }

        return timestamp - lastStarted;
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "HVAC Unit Model",
                name,
                "Analyzes zone controller output and decides what signals to send to actual HVAC hardware driver");
    }

    @Override
    public long getMinRuntime() {
        return minRuntimeMillis;
    }

    public void setMinRuntime(long minRuntimeMillis) {

        if (minRuntimeMillis != 0 && minRuntimeMillis < 1000 * 60) {
            throw new IllegalArgumentException("Unreasonably short runtime " + minRuntimeMillis + "ms, one minute is allowed. Do you remember these are milliseconds?");
        }

        this.minRuntimeMillis = minRuntimeMillis;
        stateChanged();
    }
}
