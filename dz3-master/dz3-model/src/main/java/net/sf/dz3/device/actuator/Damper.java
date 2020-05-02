package net.sf.dz3.device.actuator;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxAware;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * The damper abstraction.
 *
 * Classes implementing this interface control the hardware.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
 */
public interface Damper extends DataSink<Double>, DataSource<Double>, JmxAware {
    
    /**
     * Get damper name.
     * 
     * @return Damper name.
     */
    String getName();

    /**
     * Set the damper opening.
     *
     * This method is intentionally not made available to JMX instrumentation,
     * to avoid interference.
     * 
     * @param position 0 is fully closed, 1 is fully open, 0...1 corresponds
     * to partially open position.
     *
     * @return A token that allows to track the completion of the damper
     * movement.
     * 
     * @exception IllegalArgumentException if {@code position} is outside of 0...1 range.
     */
    Future<TransitionStatus> set(double position);
    
    /**
     * Get current damper position.
     *
     * @return Damper position.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware.
     */
    @JmxAttribute(description = "Current position")
    double getPosition() throws IOException;
    
    /**
     * Set 'park' position.
     *
     * See <a
     * href="http://sourceforge.net/tracker/index.php?func=detail&aid=916345&group_id=52647&atid=467669">bug
     * #916345</a> for more information.
     *
     * <p>
     *
     * This call doesn't cause the damper position to change, it only sets
     * the parked position preference.
     *
     * @param position A value that is considered 'parked'.
     *
     * @see #park
     * 
     * @exception IllegalArgumentException if {@code position} is outside of 0...1 range.
     */
    void setParkPosition(double position);
    
    /**
     * Get 'safe' position.
     *
     * @return A damper position that is considered 'parked'. Recommended
     * default value is 1 (fully open).
     */
    @JmxAttribute(description = "Parked position")
    double getParkPosition();
    
    /**
     * Determine whether the park position has been explicitly specified.
     *
     * See https://github.com/home-climate-control/dz/issues/51 diffs for details.
     *
     * @return {@code true} if park position was specified either via constructor, or by calling
     * {@link #setParkPosition(double)}.
     */
    boolean isCustomParkPosition();

    /**
     * 'Park' the damper.
     *
     * This call will cause the damper to move to {@link #getParkPosition
     * parked position}. Any subsequent call to {@link #set set()} will
     * unpark the damper.
     *
     * <p>
     *
     * A damper is parked in two cases: first, when the HVAC unit stops (so
     * the ventilation system can continue to work), second, when CORE shuts
     * down, so DZ can be safely disconnected and the HVAC infrastructure
     * can work without DZ's interference.
     *
     * <p>
     *
     * VT: NOTE: As ventilation aspect of DZ continues to evolve (talk to
     * Jerry Scharf), the dampers will not be parked when HVAC is shut down;
     * rather, they will be controlled by DZ's ventilation subsystem.
     *
     * @return A semaphore that is triggered when the damper is parked (it
     * may take a while if the damper is configured with a transition
     * controller).
     */
    Future<TransitionStatus> park();

    /**
     * Synchronous wrapper for {@link Damper#set(double)}.
     */
    public static class Move implements Callable<TransitionStatus> {

        private final Damper target;
        private final double position;

        public Move(Damper target, double position) {

            this.target = target;
            this.position = position;
        }

        @Override
        public TransitionStatus call() throws Exception {

            return target.set(position).get();
        }
    }

    /**
     * Utility class to move a set of dampers to given positions, synchronously or asynchronously.
     */
    public static class MoveGroup implements Callable<Future<TransitionStatus>> {

        private static final Random rg = new SecureRandom();

        /**
         * Thread pool for group transitions.
         *
         * This pool requires exactly two threads: one to scatter, one to gather.
         */
        private final ExecutorService transitionExecutor = Executors.newFixedThreadPool(2);

        protected final Logger logger = LogManager.getLogger(getClass());

        private final Map<Damper, Double> targetPosition;
        private final boolean async;

        /**
         * @param targetPosition Map between damper and positions they're supposed to be set to.
         * @param async {@code true} if the {@code Future<TransitionStatus>} will be returned immediately
         * and positions will be set in background, {@code false} if all the transitions need to end
         * before this {@link #call()} returns.
         */
        public MoveGroup(Map<Damper, Double> targetPosition, boolean async) {

            this.targetPosition = Collections.unmodifiableMap(targetPosition);
            this.async = async;
        }

        @Override
        public Future<TransitionStatus> call() throws Exception {

            final long authTokenScatter = rg.nextLong();
            final long authTokenGather  = rg.nextLong();

            TransitionStatus scatterStatus = new TransitionStatus(authTokenScatter);
            TransitionStatus gatherStatus = new TransitionStatus(authTokenGather);
            int count = targetPosition.size();
            CompletionService<TransitionStatus> cs = new ExecutorCompletionService<>(Executors.newFixedThreadPool(count));

            Runnable scatter = new Runnable() {

                @Override
                public void run() {

                    ThreadContext.push("run/scatter");

                    try {


                        for (Iterator<Entry<Damper, Double>> i = targetPosition.entrySet().iterator(); i.hasNext(); ) {

                            Entry<Damper, Double> entry = i.next();
                            Damper d = entry.getKey();
                            double position = entry.getValue();

                            logger.debug(d.getName() + ": " + position);

                            cs.submit(new Damper.Move(d, position));
                        }

                        logger.debug("fired transitions");
                        scatterStatus.complete(authTokenScatter, null);

                    } catch (Throwable t) {

                        // This is potentially expensive - may slug the HVAC if the dampers are
                        // left closed while it is running, hence fatal level

                        logger.fatal("can't set damper position", t);

                        scatterStatus.complete(authTokenScatter, t);

                    } finally {

                        ThreadContext.pop();
                        ThreadContext.clearStack();
                    }
                }
            };

            Future<TransitionStatus> scatterDone = transitionExecutor.submit(scatter, scatterStatus);

            Runnable gather = new Runnable() {

                @Override
                public void run() {

                    ThreadContext.push("run/gather");

                    try {

                        logger.debug("waiting for transitions to be fired...");

                        scatterDone.get();

                        logger.debug("gathering transition results");

                        boolean ok = true;
                        int left = count;

                        while (left-- > 0) {

                            Future<TransitionStatus> result = cs.take();
                            TransitionStatus s = result.get();

                            // This will cause the whole park() call to report failure

                            ok = s.isOK();

                            if (!s.isOK()) {

                                // This is potentially expensive - may slug the HVAC if the dampers are
                                // left closed while it is running, hence fatal level

                                logger.fatal("can't set damper position", s.getCause());
                            }
                        }

                        logger.debug("transitions complete, ok=" + ok);

                        if (ok) {

                            gatherStatus.complete(authTokenGather, null);
                            return;
                        }

                        gatherStatus.complete(authTokenGather, new IllegalStateException("one or more dampers failed to set position"));

                    } catch (Throwable t) {

                        // This is potentially expensive - may slug the HVAC if the dampers are
                        // left closed while it is running, hence fatal level

                        logger.fatal("can't set damper position", t);

                        scatterStatus.complete(authTokenGather, t);

                    } finally {

                        ThreadContext.pop();
                        ThreadContext.clearStack();
                    }
                }
            };

            Future<TransitionStatus> done = transitionExecutor.submit(gather, gatherStatus);

            if (async) {

                logger.debug("async call, bailing out");
                return done;
            }

            logger.debug("sync: waiting for completion...");

            // We need to wait until all the transitions are complete

            done.get();

            logger.debug("sync: done");

            return done;
        }
    }
}
