package net.sf.dz3.device.actuator.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.instrumentation.Marker;
import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * Damper multiplexer.
 *
 * Allows to control several physical dampers via one logical one. Each of controlled dampers
 * can be calibrated individually.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com"> Vadim Tkachenko</a> 2001-2020
 */
public class DamperMultiplexer extends AbstractDamper {

    /**
     * Dampers to control.
     */
    private final Set<Damper> dampers = new HashSet<>();

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
    protected void moveDamper(double position) throws IOException {

        multiPosition = position;

        Map<Damper, Double> targetPosition = new HashMap<>();

        for (Iterator<Damper> i = dampers.iterator(); i.hasNext(); ) {

            targetPosition.put(i.next(), position);
        }

        moveDampers(targetPosition);
    }

    private void moveDampers(Map<Damper, Double> targetPosition) throws IOException {

        transitionCompletionService.submit(new Damper.MoveGroup(targetPosition, false));

        try {

            transitionCompletionService.take().get();

        } catch (InterruptedException | ExecutionException ex) {
            throw new IOException("failed to move dampers?", ex);
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

        // VT: NOTE: This object is bogus - the whole concept needs to be revisited; see #132

        int authToken = hashCode();
        TransitionStatus result = new TransitionStatus(authToken);

        Callable<TransitionStatus> c = () -> {


            Marker m = new Marker("park/multi");
            ThreadContext.push("park/multi");

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

                    logger.debug("{}: {} ({})", d.getName(), position, custom ? "custom" : "multiplexer");

                    if (custom) {
                        logger.warn("{} will be parked at custom position {}, consider changing hardware layout so override is not necessary", d.getName(), position);
                    }
                }

                moveDampers(targetPosition);

                result.complete(authToken, null);
                return result;

            } finally {

                m.close();
                ThreadContext.pop();
                ThreadContext.clearStack();
            }
        };

        return transitionCompletionService.submit(c);
    }
}
