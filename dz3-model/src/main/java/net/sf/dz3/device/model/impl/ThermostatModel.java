package net.sf.dz3.device.model.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.controller.HysteresisController;
import net.sf.dz3.controller.ProcessController;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatController;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ThermostatStatus;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.util.digest.MessageDigestCache;
import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

/**
 * The virtual thermostat implementation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class ThermostatModel implements Thermostat, ThermostatController {

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Lowest acceptable setpoint.
     *
     * Setting it lower than this might cause water pipes freeze and burst in the winter.
     */
    public static final double SETPOINT_MIN = 10.0;

    /**
     * Highest acceptable setpoint.
     *
     * Setting the thermostat to +35C works just fine on the second floor of a house in
     * Arizona in the middle of the summer - if you want to set it higher, why don't you
     * just shut it off?
     */
    public static final double SETPOINT_MAX = 40.0;

    /**
     * Hysteresis boundaries for the {@link #signalRenderer}.
     *
     * This value is not to be fiddled with, change {@link #controller}'s P instead.
     */
    private static final double HYSTERESIS = 1.0;


    /**
     * Thermostat name.
     */
    private final String name;

    /**
     * Instrumentation signature.
     */
    private final String signature;

    /**
     * Is this zone allowed to initiate the A/C start.
     * <p/>
     * Default is true.
     *
     * @see #isVoting
     */
    private boolean voting = true;

    /**
     * The current signal.
     *
     * @see #stateChanged(AnalogSensor, DataSample<Double>)
     */
    private DataSample<Double> lastKnownSignal;

    /**
     * Current control signal.
     */
    private DataSample<Double> controlSignal = null;

    /**
     * Controller defining this thermostat's dynamic behavior.
     */
    private final AbstractPidController controller;

    /**
     * Sensor which is the data source for this thermostat,
     */
    private final AnalogSensor sensor;

    /**
     * Controller defining this thermostat's output signal.
     */
    private final HysteresisController signalRenderer = new HysteresisController(0, HYSTERESIS);

    /**
     * Thermostat signal broadcaster.
     */
    private final DataBroadcaster<ThermostatSignal> dataBroadcaster = new DataBroadcaster<ThermostatSignal>();

    /**
     * Is this thermostat enabled.
     * <p/>
     * If the zone is set to "OFF", the thermostat will still accept the
     * notifications, but will not relay the signal to the controller.
     */
    private boolean tsEnabled = true;

    /**
     * Is this thermostat on hold.
     * <p/>
     * If it is set to "hold", the scheduler will not change the settings when
     * the period changes.
     */
    private boolean hold = false;

    /**
     * Is it possible to use the zone controlled by this thermostat as a dump
     * zone.
     * <p/>
     * 0 (default) means that this zone shouldn't be used as a dump zone, values
     * greater than 0 mean priority, 1 being the highest. The higher the
     * priority, the sooner the zone will be used as a dump zone when heating.
     */
    private int dumpPriority = 0;

    /**
     * Full constructor - creates an instance with an arbitrary implementation of {@link AbstractPidController}.
     *
     * @param name Human readable name for the zone this thermostat is in. This will be
     * displayed on the user interface.
     * @param sensor Sensor to read the data from.
     * @param controller Process controller to use.
     */
    public ThermostatModel(String name, AnalogSensor sensor, AbstractPidController controller) {

        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("name can't be null or empty");
        }

        if (sensor == null) {
            throw new IllegalArgumentException("sensor can't be null or empty");
        }

        if (controller == null) {
            throw new IllegalArgumentException("controller can't be null");
        }

        this.name = name;
        this.signature = MessageDigestCache.getMD5(name).substring(0, 19);
        this.controller = controller;
        this.sensor = sensor;

        sensor.addConsumer(this);
    }

    /**
     * Abbreviated constructor - creates an instance with an instance of {@link SimplePidController}
     * created on the fly.
     *
     * @param name Human readable name for the zone this thermostat is in. This will be
     * displayed on the user interface.
     * @param sensor Sensor to read the data from.
     * @param setpoint Process controller setpoint.
     * @param P Process controller P component.
     * @param I Process controller I component.
     * @param D Process controller D component.
     * @param saturationLimit Process controller saturation limit.
     */
    public ThermostatModel(String name, AnalogSensor sensor, double setpoint, double P, double I, double D, double saturationLimit) {

        this(name, sensor, new SimplePidController(setpoint, P, I, D, saturationLimit));
    }

    @Override
    public String getName() {

	return name;
    }

    @Override
    public void setVoting(boolean voting) {

	logger.info("setVoting: " + voting);
	this.voting = voting;
	stateChanged();
    }

    @Override
    public boolean isVoting() {

	return voting;
    }

    @Override
    public void setOnHold(boolean hold) {

	logger.info("setOnHold: " + hold);
	this.hold = hold;
	stateChanged();
    }

    @Override
    public boolean isOnHold() {

	return hold;
    }

    @Override
    public void setOn(boolean enabled) {

	logger.info("setOn: " + enabled);

	this.tsEnabled = enabled;
	stateChanged();
    }

    @Override
    public boolean isOn() {

	return tsEnabled;
    }

    @Override
    public boolean isError() {
	return lastKnownSignal != null && lastKnownSignal.isError();
    }

    @Override
    public void setDumpPriority(int dumpPriority) {
	this.dumpPriority = dumpPriority;
    }

    @Override
    public int getDumpPriority() {

        // VT: NOTE: The commented out section below causes thermostat settings
        // to be improperly identified as altered in ThermostatPanel (because
        // dump priority doesn't match). Following Occam's Razor, this behavior
        // is disabled - if someone wants their off zones to serve as dump zones,
        // let them specify this explicitly instead of wondering how the hell
        // did the zones became dump zones without being asked to.

//	if (!isOn()) {
//
//	    // Override. If the zone is shut off, it can be used as a dump zone.
//
//	    return 1;
//	}

	return dumpPriority;
    }

    @Override
    public synchronized void consume(DataSample<Double> sample) {

        ThreadContext.push("consume");

        try {

            if (sample == null) {
                throw new IllegalArgumentException("sample can't be null");
            }

            lastKnownSignal = sample;

            // VT: FIXME: At this point, we'll just complain if the reading is
            // not available. Later, we'll have to open the damper for this zone
            // and exclude it from the zone controller logic.

            if (sample.isError()) {

                logger.warn("Sensor failure: " + sample);

                // Can't recalculate the control signal in this case,
                // but need to notify the zone controller.

                stateChanged();
                return;
            }

            // VT: NOTE: Do not be tempted to remove compute() from the logic if
            // this zone is not tsEnabled - it may seem a pointless calculation,
            // but it's not. Since the process controller is a time sensitive
            // entity, it will keep calculating the *correct* value of the
            // signal, which needs to be done even if it is not currently used.

            // VT: NOTE: Hmm... Which time value to take? The sample comes with its own,
            // but there may be a delay. Let's see how it works with the original timestamp -
            // all in all, it should've been delivered back then.

            controlSignal = controller.compute(sample);

            signalRenderer.compute(controlSignal);

            logger.debug("status: " + this);
            stateChanged();

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Get the adjusted {@link #controlSignal control signal} value.
     *
     * @return If {@link #tsEnabled tsEnabled}, the value of the
     *         {@link #controlSignal control signal}, otherwise 0.
     */
    @Override
    public double getControlSignal() {

	return tsEnabled ? (controlSignal == null ? 0 : controlSignal.sample) : 0;
    }

    public ProcessController getController() {

	return controller;
    }

    public AnalogSensor getSensor() {

        return sensor;
    }

    @Override
    public void raise() {

        // Bump it up for good
        signalRenderer.consume(new DataSample<Double>("internal", signature, HYSTERESIS * 2, null));
    }

    @Override
    public double getSetpoint() {
	return controller.getSetpoint();
    }

    @Override
    public void setSetpoint(double setpoint) {

        ThreadContext.push("setSetpoint");

        try {

            if (setpoint > SETPOINT_MAX || setpoint < SETPOINT_MIN) {
                throw new IllegalArgumentException("Value (" + setpoint + ") is outside of sane range ("
                        + SETPOINT_MIN + "..." + SETPOINT_MAX + ")");
            }

	    logger.debug("Old setpoint: " + getSetpoint());
            logger.info("New setpoint: " + setpoint);

            controller.setSetpoint(setpoint);

	} finally {
	    ThreadContext.pop();
	}

    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("Thermostat[").append(getName()).append(", ").append(getStatus()).append("]");

        return sb.toString();
    }

    @Override
    public int compareTo(Thermostat other) {

	if (other == null) {
	    throw new IllegalArgumentException("other can't be null");
	}

	return getName().compareTo(other.getName());
    }

    @JmxAttribute(description = "Thermostat status")
    public ThermostatStatus getStatus() {
	return new ThermostatStatusImpl(getSetpoint(), getControlSignal(), getDumpPriority(), isOn(), isOnHold(), isVoting(), isError());
    }

    private void stateChanged() {
        ThermostatSignal signal = getSignal();

        // VT: NOTE: This will not be an error signal even if the original signal is,
        // the purpose is not control but instrumentation

        DataSample<ThermostatSignal> sample = new DataSample<ThermostatSignal>(signal.demand.timestamp,
                signal.demand.sourceName, signal.demand.signature, signal, null);

        dataBroadcaster.broadcast(sample);
    }

    @Override
    public ThermostatSignal getSignal() {

        if (!isOn()) {

            // Demand is always off no matter what is happening to the setpoint,
            // but the control logic still keeps working so the moment the thermostat is back on,
            // the rest of the system will kick in  without a jolt

            return new ThermostatSignal(
                    false,
                    isOnHold(),
                    false,
                    isVoting(),
                    new DataSample<Double>(controlSignal.timestamp, getName(), signature, 0d, null));
        }

        if (lastKnownSignal == null) {

            // Must be before receiving any data at all

            return new ThermostatSignal(
                    isOn(),
                    isOnHold(),
                    signalRenderer.getState(),
                    voting,
                    new DataSample<Double>(System.currentTimeMillis(), getName(), signature, null, new IllegalStateException("No data received yet")));
        }

        if (lastKnownSignal.isError()) {

            return new ThermostatSignal(
                    isOn(),
                    isOnHold(),
                    signalRenderer.getState(),
                    voting,
                    new DataSample<Double>(lastKnownSignal.timestamp, getName(), signature, null, lastKnownSignal.error));
        }

        assert(controlSignal != null);

        return new ThermostatSignal(
                isOn(),
                isOnHold(),
                signalRenderer.getState(),
                voting,
                new DataSample<Double>(controlSignal.timestamp, getName(), signature, controlSignal.sample - signalRenderer.getThresholdLow(), null));
    }

    @Override
    public void addConsumer(DataSink<ThermostatSignal> consumer) {

    	if (consumer instanceof ZoneController) {

    		StackTraceElement[] trace = Thread.currentThread().getStackTrace();

    		boolean ok = false;

    		for (int offset = 0; offset < trace.length; offset++) {

    			StackTraceElement frame = trace[offset];

    			if (frame.getClassName().equals("net.sf.dz3.device.model.impl.SimpleZoneController")) {

    				// It is OK if the zone controller adds itself as a listener from inside the constructor

    				ok = true;
    				break;
    			}
    		}

    		if (!ok) {

    			// A thermostat must be registered with the zone controller in order for the controller
    			// to make correct decisions. If a controller is simply added as a consumer, it won't recognize
    			// this thermostat as a source and will refuse to take the signal from it.

    			logger.error("ZoneController is being added as a consumer, is this really what you want?",
    					new IllegalArgumentException("Read source for details"));
    		}
    	}

        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<ThermostatSignal> consumer) {

        dataBroadcaster.removeConsumer(consumer);
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "Thermostat",
                getName(),
                "Tell the zone controller what to do based on current sensor data, setpoint, and fine tuning parameters");
    }

    @Override
    public void set(ZoneStatus status) {

        ThreadContext.push("set");

        try {

            if (isOnHold()) {

                logger.info("On hold, set(" + status + ") ignored");
                return;
            }

            setSetpoint(status.getSetpoint());
            setOn(status.isOn());
            setVoting(status.isVoting());
            setDumpPriority(status.getDumpPriority());

        } finally {
            ThreadContext.pop();
        }
    }
}
