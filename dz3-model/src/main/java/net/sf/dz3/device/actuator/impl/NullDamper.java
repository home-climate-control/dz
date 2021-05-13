package net.sf.dz3.device.actuator.impl;

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

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
	
	ThreadContext.push("moveDamper");

	try {
	
	    logger.debug("new position: " + throttle);
	    this.throttle = throttle;

	} finally {
	    ThreadContext.pop();
	}
    }

    public double getPosition() throws IOException {
	
	ThreadContext.push("getThrottle");
	
	try {

	    logger.debug("returning: " + throttle);
	    return throttle;
        
	} finally {
	    ThreadContext.pop();
	}
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Null damper",
                Integer.toHexString(hashCode()),
                "Does absolutely nothing");
    }
}
