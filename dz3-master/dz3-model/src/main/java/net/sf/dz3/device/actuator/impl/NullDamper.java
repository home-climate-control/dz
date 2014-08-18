package net.sf.dz3.device.actuator.impl;

import java.io.IOException;

import net.sf.jukebox.jmx.JmxDescriptor;

import org.apache.log4j.NDC;

/**
 * 'Null' damper - this damper does not interact with hardware, just logs
 * activities.
 *
 * Useful for debugging/development, uncontrolled zones, or single zone
 * systems.
 *
 * @author <a href="mailto:tim at buttersideup dot company">Tim Small</a>
 */
public class NullDamper extends AbstractDamper {

    public NullDamper(String name) {
        super(name);
    }

    /**
     * Current throttle value.
     */
    private double throttle = 0.3;

    @Override
    public void moveDamper(double throttle) {
	
	NDC.push("moveDamper");

	try {
	
	    logger.debug("new position: " + throttle);
	    this.throttle = throttle;

	} finally {
	    NDC.pop();
	}
    }

    public double getPosition() throws IOException {
	
	NDC.push("getThrottle");
	
	try {

	    logger.debug("returning: " + throttle);
	    return throttle;
        
	} finally {
	    NDC.pop();
	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Null damper",
                Integer.toHexString(hashCode()),
                "Does absolutely nothing");
    }
}
