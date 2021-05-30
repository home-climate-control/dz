package net.sf.dz3.device.actuator.impl;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import com.homeclimatecontrol.jukebox.sem.ACT;
import com.homeclimatecontrol.jukebox.sem.SemaphoreGroup;
import com.homeclimatecontrol.jukebox.service.Messenger;
import net.sf.dz3.device.actuator.Damper;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Damper multiplexer.
 *
 * Allows to control several physical dampers via one logical one. Each of controlled dampers
 * can be calibrated individually.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
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
    protected synchronized void moveDamper(double position) throws IOException {

        for (Damper d : dampers) {

            try {

                d.set(position);

            } catch (IOException ex) {

                // VT: NOTE: Multiplexer is less prone to errors than a regular damper,
                // because different dampers may be controlled by different controllers and
                // not fail all at once. However, low probability of this happening
                // makes it impractical to handle such errors separately. If you feel otherwise,
                // feel free to interfere (i.e. not throw an exception if not all dampers failed).

                throw new IOException("One of controlled dampers failed", ex);
            }
        }

        // For fairness sake, let's set this bogus thing *after* we're done
        multiPosition = position;
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
    public ACT park() {
        return new ParkingAssistant().start();
    }

    /**
     * Commands the {@link #dampers} to move to their{@link Damper#getParkPosition
     * parked position} and waits until the dampers have done so.
     */
    private class ParkingAssistant extends Messenger {

        @Override
        protected final Object execute() throws Throwable {

            ThreadContext.push("execute");

            try {

                var parked = new SemaphoreGroup();

                for (Damper d : dampers) {
                    parked.add(d.park());
                }

                logger.info("{}: parking...", getName());
                parked.waitForAll();

                logger.info("{}: parked", getName());

                // For fairness sake, let's set this bogus thing *after* we're done
                multiPosition = getParkPosition();

            } finally {
                ThreadContext.pop();
            }

            return null;
        }
    }
}
