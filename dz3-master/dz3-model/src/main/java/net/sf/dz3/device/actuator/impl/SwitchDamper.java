package net.sf.dz3.device.actuator.impl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.instrumentation.Marker;
import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.servomaster.device.model.TransitionStatus;

/**
 * Damper controlled by a switch.
 * 
 * Most existing HVAC dampers are like this, controlled by 24VAC.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2018
 */
public class SwitchDamper extends AbstractDamper {

    /**
     * Current position.
     */
    private double position;
    
    /**
     * Hardware switch that controls the actual damper.
     */
    private final Switch target;
    
    /**
     * Switch threshold.
     * 
     * Values passed to {@link #moveDamper(double)} above the threshold
     * will set the switch to 1,values equal or less will set the switch to 0.
     */
    private double threshold;

    /**
     * Create an instance with default (1.0) park position.
     * 
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     */
    public SwitchDamper(String name, Switch target, double threshold) {
        
        this(name, target, threshold, 1.0);
    }

    /**
     * Create an instance.
     * 
     * @param name Damper name. Necessary evil to allow instrumentation signature.
     * @param target Switch that controls the actual damper.
     * @param threshold Switch threshold.
     * @param parkPosition Damper position defined as 'parked'.
     */
    public SwitchDamper(String name, Switch target, double threshold, double parkPosition) {
        super(name);
        
        check(target);
        check(threshold);
        
        this.target = target;
        this.threshold = threshold;
        
        setParkPosition(parkPosition);
        
        set(getParkPosition());
    }

    private void check(Switch target) {
        
        if (target == null) {
            throw new IllegalArgumentException("target can't be null");
        }
    }

    private void check(double threshold) {
        
        if (threshold <= 0 || threshold >= 1.0 ) {
            throw new IllegalArgumentException("Unreasonable threshold value given ("
                    + threshold + "), valid values are (0 < threshold < 1)");
        }
    }

    @Override
    public Future<TransitionStatus> moveDamper(double position) {

        ThreadContext.push("moveDamper");

        // VT: NOTE: For now, let's assume that transition is instantaneous.
        // However, let's keep an eye on it - there may be slow acting switches
        // such as ShellSwitch.

        Marker m = new Marker("moveDamper");
        TransitionStatus status = new TransitionStatus(m.hashCode());

        try {

            boolean state = position > threshold ? true : false;

            logger.debug("translated " + position + " => " + state);

            target.setState(state);

            this.position = position;

            status.complete(m.hashCode(), null);

        } catch (Throwable t) {

            // This is pretty serious, closed damper may cause the compressor to slug
            // or the boiler to blow up
            logger.fatal("Failed to set the damper state", t);

            status.complete(m.hashCode(), t);

        } finally {

            m.close();
            ThreadContext.pop();
        }

        return CompletableFuture.completedFuture(status);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPosition() throws IOException {
        
        return position; 
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Switch based damper",
                Integer.toHexString(hashCode()),
                "Controls a switch that controls a damper");
    }
}
