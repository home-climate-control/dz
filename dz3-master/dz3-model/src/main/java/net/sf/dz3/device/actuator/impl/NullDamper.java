package net.sf.dz3.device.actuator.impl;

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * 'Null' damper - this damper does not interact with hardware, just logs
 * activities.
 *
 * Useful for debugging/development, uncontrolled zones, or single zone
 * systems.
 *
 * @author <a href="mailto:tim at buttersideup dot company">Tim Small</a>
 * @author improvements Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com"> Vadim Tkachenko</a> 2001-2020
 */
public class NullDamper extends AbstractDamper {

    /**
     * Current throttle value.
     */
    private double throttle;

    public NullDamper(String name) {
        super(name);

        throttle = getParkPosition();
    }

    @Override
    public void moveDamper(double throttle) throws IOException {

	ThreadContext.push("moveDamper");

	try {

	    logger.debug("new position: " + throttle);
	    this.throttle = throttle;

	} finally {
	    ThreadContext.pop();
	}
    }

    @Override
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
