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
     * Our own position. Different from {@link AbstractDamper#position}.
     *
     * This variable doesn't participate in transitions, but is rather for reporting purposes.
     */
    private double multiPosition;

    /**
     * Create an instance.
     * 
     * @param name Name to use.
     * @param dampers Set of dampers to control.
     * @param parkPosition Park position.
     */
    public DamperMultiplexer(String name, Set<Damper> dampers, Double parkPosition) {
        super(name);

        this.dampers.addAll(dampers);

        if (parkPosition != null) {
            setParkPosition(parkPosition);
        }

        multiPosition = getParkPosition();
    }

    /**
     * Create an instance with default parking position.
     *
     * @param name Name to use.
     * @param dampers Set of dampers to control.
     */
    public DamperMultiplexer(String name, Set<Damper> dampers) {

        this(name, dampers, null);
    }

    @Override
    protected synchronized Future<TransitionStatus> moveDamper(double position) {

        multiPosition = position;

        Map<Damper, Double> targetPosition = new HashMap<>();

        for (Iterator<Damper> i = dampers.iterator(); i.hasNext(); ) {

            targetPosition.put(i.next(), position);
        }

        return moveDampers(targetPosition);
    }

    private Future<TransitionStatus> moveDampers(Map<Damper, Double> targetPosition) {

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
    public synchronized double getPosition() throws IOException {
        
        return multiPosition;
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Damper multiplexer",
                Integer.toHexString(hashCode()),
                "Controls " + dampers.size() + " dampers");
    }

    @Override
    public Future<TransitionStatus> park() {

        ThreadContext.push("park");

        try {

            multiPosition = getParkPosition();

            // Need to park dampers at their individual positions if they were specified,
            // or to the multiplexer parking position if they weren't.

            Map<Damper, Double> targetPosition = new HashMap<>();

            for (Iterator<Damper> i = dampers.iterator(); i.hasNext(); ) {

                Damper d = i.next();
                boolean custom = d.isCustomParkPosition();
                double position = custom ? d.getParkPosition() : getParkPosition();

                targetPosition.put(d, position);

                logger.debug(d.getName() + ": " + position + " (" + (custom ? "custom" : "multiplexer") + ")");

                if (custom) {

                    // Dude, you better know what you're doing

                    logger.warn(d.getName() + " will be parked at custom position, consider changing hardware layout so override is not necessary");
                }
            }

            return moveDampers(targetPosition);

        } finally {
            ThreadContext.pop();
        }
    }
}
