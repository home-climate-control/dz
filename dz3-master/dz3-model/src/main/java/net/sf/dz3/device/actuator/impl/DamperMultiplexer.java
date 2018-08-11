package net.sf.dz3.device.actuator.impl;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.actuator.Damper;
import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * Damper multiplexer.
 * 
 * Allows to control several physical dampers via one logical one. Each of controlled dampers
 * can be calibrated individually.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2018
 */
public class DamperMultiplexer extends AbstractDamper {

    private static final Random rg = new SecureRandom();

    /**
     * Thread pool for parking assistants.
     *
     * This pool requires exactly one thread.
     */
    private final ExecutorService parkingExecutor = Executors.newFixedThreadPool(1);

    /**
     * Dampers to control.
     */
    private final Set<Damper> dampers = new HashSet<Damper>();
    
    /**
     * Create an instance.
     * 
     * @param name Name to use.
     * @param dampers Set of dampers to control.
     */
    public DamperMultiplexer(String name, Set<Damper> dampers) {
        super(name);
        
        this.dampers.addAll(dampers);
    }

    @Override
    protected synchronized void moveDamper(double position) throws IOException {
        
        for (Iterator<Damper> i = dampers.iterator(); i.hasNext(); ) {
            
            Damper d = i.next();
            
            // VT: FIXME: Not possible to all things in one commit.
            // https://github.com/home-climate-control/dz/issues/49

            d.set(position);
        }
    }

    @Override
    public double getPosition() throws IOException {
        
        // A random one (HashSet, remember?) is as good as any other,
        // they're supposed to be identical anyway
        
        return dampers.iterator().next().getPosition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Damper multiplexer",
                Integer.toHexString(hashCode()),
                "Controls " + dampers.size() + " dampers");
    }

    /**
     * {@inheritDoc}
     */
    public Future<TransitionStatus> park() {

        ThreadContext.push("park");

        try {

            final long authToken = rg.nextLong();
            final TransitionStatus status = new TransitionStatus(authToken);

            Runnable parkingAssistant = new Runnable() {

                @Override
                public void run() {

                    ThreadContext.push("run");

                    try {

                        logger.info(getName() + ": parking at " + getParkPosition());

                        // VT: NOTE: Ignoring state consistency of the damper collection

                        int count = dampers.size();
                        CompletionService<TransitionStatus> cs = new ExecutorCompletionService<>(Executors.newFixedThreadPool(count));

                        for (Iterator<Damper> i = dampers.iterator(); i.hasNext(); ) {

                            Damper d = i.next();

                            // VT: FIXME: https://github.com/home-climate-control/dz/issues/41
                            //
                            // Need to park dampers at their individual positions if they were specified,
                            // or to the multiplexer parking position if they weren't.
                            //
                            // At this point, it is not known whether the parking positions were specified or not
                            // (the parking position is double, not Double); need to fix this.

                            cs.submit(new Damper.Move(d, getParkPosition()));
                        }

                        boolean ok = true;

                        while (count-- > 0) {

                            Future<TransitionStatus> result = cs.take();
                            TransitionStatus s = result.get();

                            // This will cause the whole park() call to report failure

                            ok = s.isOK();

                            if (!ok) {

                                // This is potentially expensive - may slug the HVAC if the dampers are
                                // left closed while it is running, hence fatal level

                                logger.fatal("can't park one of the dampers", s.getCause());
                            }
                        }

                        if (ok) {

                            status.complete(authToken, null);
                            return;
                        }

                        status.complete(authToken, new IllegalStateException("one or more dampers failed to park"));

                    } catch (Throwable t) {

                        // This is potentially expensive - may slug the HVAC if the dampers are
                        // left closed while it is running, hence fatal level

                        logger.fatal("can't park", t);

                        status.complete(authToken, t);

                    } finally {

                        ThreadContext.pop();
                        ThreadContext.clearStack();
                    }
                }
            };

            return parkingExecutor.submit(parkingAssistant, status);

        } finally {
            ThreadContext.pop();
        }
    }
}
