package net.sf.dz3.device.actuator.impl;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.logger.LogAware;
import com.homeclimatecontrol.jukebox.sem.ACT;
import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.util.digest.MessageDigestCache;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;

/**
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractDamper extends LogAware implements Damper {

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
     * A damper position defined as 'parked'.
     *
     * Default value is 1 (fully open).
     */
    private double parkPosition = 1;

    /**
     * Current position.
     */
    private double position = parkPosition;

    AbstractDamper(String name) {

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
        return parkPosition;
    }

    @SuppressWarnings("squid:S1181")
    @Override
    public final void set(double throttle) {

        ThreadContext.push("set");

        try {

            logger.info("position={}", throttle);

            if ( throttle < 0 || throttle > 1.0 || Double.compare(throttle, Double.NaN) == 0) {

                throw new IllegalArgumentException("Throttle out of 0...1 range: " + throttle);
            }

            this.position = throttle;

            try {

                moveDamper(throttle);
                stateChanged();

            } catch (Throwable t) {

                // squid:S1181: No.
                logger.error("Failed to move damper to position {}", throttle, t);
                // VT: FIXME: Need to change Damper to be a producer of DataSample<Double>, not Double
                stateChanged();
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Move the actual damper.
     *
     * @param position Position to set.
     *
     * @exception IOException if there was a problem moving the damper.
     */
    protected abstract void moveDamper(double position) throws IOException;

    @SuppressWarnings("squid:S1181")
    @Override
    public ACT park() {

        ThreadContext.push("park");

        try {

            // VT: NOTE: Careful here. This implementation will work correctly only if
            // moveDamper() works synchronously. For others (ServoDamper being a good example)
            // you will have to provide your own implementation (again, ServoDamper is an
            // example of how this is done).

            var done = new ACT();

            try {

                moveDamper(parkPosition);
                done.complete(true);

            } catch (Throwable t) {

                // squid:S1181: No.
                done.complete(false);
            }

            return done;

        } finally {
            ThreadContext.pop();
        }
    }

    private synchronized void stateChanged() {

        dataBroadcaster.broadcast(new DataSample<>(System.currentTimeMillis(), name, signature, position, null));
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
}
