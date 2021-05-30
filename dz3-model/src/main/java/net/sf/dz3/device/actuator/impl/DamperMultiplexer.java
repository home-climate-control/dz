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
    }

    @Override
    public double getPosition() throws IOException {

        // A random one (HashSet, remember?) is as good as any other,
        // they're supposed to be identical anyway

        return dampers.iterator().next().getPosition();
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

            } finally {
                ThreadContext.pop();
            }

            return null;
        }
    }
}
