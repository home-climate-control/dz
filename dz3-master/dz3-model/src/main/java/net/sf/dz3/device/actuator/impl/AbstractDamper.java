package net.sf.dz3.device.actuator.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.util.digest.MessageDigestCache;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.logger.LogAware;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2018
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

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();

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

        return parkPosition;
    }

    @Override
    public final Future<TransitionStatus> set(double throttle) {
        
        ThreadContext.push("set");

        try {
            
            logger.info("position=" + throttle);

            if ( throttle < 0 || throttle > 1.0 || Double.compare(throttle, Double.NaN) == 0) {

                throw new IllegalArgumentException("Throttle out of 0...1 range: " + throttle);
            }

            this.position = throttle;

            try {

                Future<TransitionStatus> done = moveDamper(throttle);
                stateChanged();
                
                return done;

            } catch (Throwable t) {

                logger.fatal("Failed to move damper to position " + throttle, t);

                // VT: FIXME: Need to change Damper to be a producer of DataSample<Double>, not Double
                stateChanged();

                TransitionStatus done = new TransitionStatus(t.hashCode());
                done.complete(t.hashCode(), t);

                return CompletableFuture.completedFuture(done);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Move the actual damper.
     *
     * @param position Position to set.
     */
    protected abstract Future<TransitionStatus> moveDamper(double position);

    @Override
    public Future<TransitionStatus> park() {

        return set(getParkPosition());
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
}
