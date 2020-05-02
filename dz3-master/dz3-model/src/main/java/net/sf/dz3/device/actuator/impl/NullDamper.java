package net.sf.dz3.device.actuator.impl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.apache.logging.log4j.ThreadContext;

import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * 'Null' damper - this damper does not interact with hardware, just logs
 * activities.
 *
 * Useful for debugging/development, uncontrolled zones, or single zone
 * systems.
 *
 * @author <a href="mailto:tim at buttersideup dot company">Tim Small</a>
 * @author improvements Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2018
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
    public Future<TransitionStatus> moveDamper(double throttle) {
	
	ThreadContext.push("moveDamper");

	try {
	
	    logger.debug("new position: " + throttle);
	    this.throttle = throttle;

	    TransitionStatus status = new TransitionStatus(0);

	    status.complete(0, null);

	    return CompletableFuture.completedFuture(status);

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
