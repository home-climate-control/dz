package net.sf.dz3.device.actuator.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
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

    /**
     * Completion service for asynchronous transitions.
     *
     * This pool requires exactly one thread.
     */
    CompletionService<Future<TransitionStatus>> transitionCompletionService = new ExecutorCompletionService<>(Executors.newFixedThreadPool(1));

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
    protected synchronized Future<TransitionStatus> moveDamper(double position) {

        Map<Damper, Double> targetPosition = new HashMap<>();

        for (Iterator<Damper> i = dampers.iterator(); i.hasNext(); ) {

            targetPosition.put(i.next(), position);
        }

        transitionCompletionService.submit(new Damper.MoveGroup(targetPosition, false));

        try {

            // VT: NOTE: The following line unwraps one level of Future. The first Future
            // is completed when the transitions have been fired, and the second is
            // when they all complete.

            return transitionCompletionService.take().get();

        } catch (InterruptedException | ExecutionException ex) {

            // VT: FIXME: Oops... Really don't know what to do with this, will have to collect stats
            // before this can be reasonably handled

            throw new IllegalStateException("Unhandled exception", ex);
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

            // VT: FIXME: https://github.com/home-climate-control/dz/issues/41
            //
            // Need to park dampers at their individual positions if they were specified,
            // or to the multiplexer parking position if they weren't.
            //
            // At this point, it is not known whether the parking positions were specified or not
            // (the parking position is double, not Double); need to fix this.

            return moveDamper(getParkPosition());

        } finally {
            ThreadContext.pop();
        }
    }
}
