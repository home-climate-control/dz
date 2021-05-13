package net.sf.dz3.device.actuator.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.actuator.Damper;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import com.homeclimatecontrol.jukebox.sem.ACT;
import com.homeclimatecontrol.jukebox.sem.SemaphoreGroup;
import com.homeclimatecontrol.jukebox.service.Messenger;

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

        // VT: This implementation is similar to the one used in ServoDamper,
        // but abstractions are different.

        logger.info(getName() + ": parking at " + getParkPosition());

        return new ParkingAssistant().start();
    }

    /**
     * Commands the {@link ServoDamper#servo} to move to {@link ServoDamper#getParkPosition
     * parked position} and waits until the servo has done so.
     */
    private class ParkingAssistant extends Messenger {

        /**
         * Move the {@link ServoDamper#servo} and wait until it gets there.
         */
        @Override
        protected final Object execute() throws Throwable {

            ThreadContext.push("execute");
            
            try {
                
                SemaphoreGroup parked = new SemaphoreGroup();
                
                for (Iterator<Damper> i = dampers.iterator(); i.hasNext(); ) {
                    
                    Damper d = i.next();
                    
                    parked.add(d.park());
                }

                parked.waitForAll();

                logger.info(getName() + ": parked at " + getParkPosition());

            } catch (Throwable t) {

                logger.error(getName() + ": failed to park at " + getParkPosition(), t);
                
            } finally {
                ThreadContext.pop();
            }

            return null;
        }
    }
}
