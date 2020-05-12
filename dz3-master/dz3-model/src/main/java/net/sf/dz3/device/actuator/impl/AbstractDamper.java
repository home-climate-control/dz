package net.sf.dz3.device.actuator.impl;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.util.digest.MessageDigestCache;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com"> Vadim Tkachenko</a> 2001-2020
 */
public abstract class AbstractDamper implements Damper {

    protected final Logger logger = LogManager.getLogger(getClass());

    /**
     * Completion service for asynchronous transitions.
     *
     * This pool requires exactly one thread, to honor the happened-before relation
     * between the series of commands sent to this damper.
     */
    ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Damper name.
     *
     * Necessary evil to allow instrumentation signature.
     */
     private final String name;

     /**
      * Instrumentation signature.
      */
     private final String signature;

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<>();

    /**
     * Position to park if there was no park position {@link #setParkPosition(double) explicitly specified}.
     *
     * Normally, the damper should be fully open in this position.
     */
    private double defaultParkPosition = 1.0;

    /**
     * A damper position defined as 'parked'.
     *
     * Default is {@code null} - none. See commits related to https://github.com/home-climate-control/dz/issues/51
     * for more details.
     *
     * If the value is {@code null} and {@link #park()} method is called, the value of
     * {@link #defaultParkPosition} is used.
     */
    private Double parkPosition = null;

    /**
     * Current position.
     */
    private double position = defaultParkPosition;

    public AbstractDamper(String name) {

        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("name can't be null");
        }

        this.name = name;
        signature = MessageDigestCache.getMD5(name).substring(0, 19);
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final void setParkPosition(double parkPosition) {

        if (parkPosition < 0 || parkPosition > 1) {

            throw new IllegalArgumentException("Invalid position (" + parkPosition + ") - value can be 0..1.");
        }

        this.parkPosition = parkPosition;
    }

    @Override
    public final double getParkPosition() {

        return parkPosition == null ? defaultParkPosition : parkPosition;
    }

    @Override
    public boolean isCustomParkPosition() {

        return parkPosition != null;
    }

    @Override
    public final Future<TransitionStatus> set(double throttle) {

        // VT: NOTE: This object is bogus - the whole concept needs to be revisited; see #132

        int authToken = hashCode();
        TransitionStatus result = new TransitionStatus(authToken);

        Callable<TransitionStatus> c = () -> {

            ThreadContext.push("set/" + getName());

            try {

                logger.info("position={}", throttle);

                if ( throttle < 0 || throttle > 1.0 || Double.compare(throttle, Double.NaN) == 0) {

                    throw new IllegalArgumentException("Throttle out of 0...1 range: " + throttle);
                }

                this.position = throttle;

                moveDamper(throttle);
                stateChanged();

                result.complete(authToken, null);

                return result;

            } finally {
                ThreadContext.pop();
            }
        };

        return executor.submit(c);
    }

    /**
     * Move the actual damper.
     *
     * @param position Position to set.
     *
     * @exception IOException if there was a problem moving the damper.
     */
    protected abstract void moveDamper(double position) throws IOException;

    @Override
    public Future<TransitionStatus> park() {

        ThreadContext.push("park/" + getName());

        try {

            logger.debug("parking at {}", getParkPosition());

            return set(getParkPosition());

        } finally {
            ThreadContext.pop();
        }
    }

    private synchronized void stateChanged() {

        dataBroadcaster.broadcast(new DataSample<Double>(System.currentTimeMillis(), name, signature, position, null));
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {

        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {

        dataBroadcaster.removeConsumer(consumer);
    }

    @Override
    public void consume(DataSample<Double> signal) {

        set(signal.sample);
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "@" + hashCode() + "(" + getName() + ")";
    }
}
