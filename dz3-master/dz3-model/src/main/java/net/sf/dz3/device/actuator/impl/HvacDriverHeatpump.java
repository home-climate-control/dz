package net.sf.dz3.device.actuator.impl;

import java.io.IOException;

import org.apache.log4j.NDC;

import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.sensor.Switch;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Single stage heatpump driver, energize to heat.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class HvacDriverHeatpump extends AbstractHvacDriver {
    
    private final Switch switchMode;
    private final Switch switchRunning;
    private final Switch switchFan;
    
    private final boolean reverseMode;
    private final boolean reverseRunning;
    private final boolean reverseFan;
    
    private boolean enabled = true;
    
    /**
     * Create an instance with all straight switches.
     * 
     * @param switchMode Switch to pull to change the operating mode.
     * @param switchRunning Switch to pull to turn on the compressor.
     * @param switchFan Switch to pull to turn on the air handler.
     */
    public HvacDriverHeatpump(Switch switchMode, Switch switchRunning, Switch switchFan) {
        
        this(switchMode, false, switchRunning, false, switchFan, false);
    }
    
    /**
     * Create an instance with some switches possibly reverse.
     * 
     * @param switchMode Switch to pull to change the operating mode.
     * @param reverseMode {@code true} if the "off" mode position corresponds to logical one.
     * @param switchRunning Switch to pull to turn on the compressor.
     * @param reverseMode {@code true} if the "off" running position corresponds to logical one.
     * @param switchFan Switch to pull to turn on the air handler.
     * @param reverseMode {@code true} if the "off" fan position corresponds to logical one.
     */
    public HvacDriverHeatpump(
            Switch switchMode, boolean reverseMode,
            Switch switchRunning, boolean reverseRunning,
            Switch switchFan, boolean reverseFan) {
        
        check(switchMode, "mode");
        check(switchRunning, "running");
        check(switchFan, "fan");
        
        this.switchMode = switchMode;
        this.switchRunning = switchRunning;
        this.switchFan = switchFan;
        
        this.reverseMode = reverseMode;
        this.reverseRunning = reverseRunning;
        this.reverseFan = reverseFan;
    }

    private void check(Switch s, String purpose) {
        
        if (s == null) {
            throw new IllegalArgumentException(purpose + "Switch can't be null");
        }
    }

    @Override
    protected synchronized void doSetMode(HvacMode mode) throws IOException {

        checkEnabled();
        
        boolean state = HvacMode.HEATING == mode;
        switchMode.setState(reverseMode ? !state : state);
    }

    @Override
    protected synchronized void doSetStage(int stage) throws IOException {
        
        checkEnabled();
        
        boolean state = stage > 0 ? true: false;
        switchRunning.setState(reverseRunning ? !state : state);
    }

    @Override
    protected synchronized void doSetFanSpeed(double speed) throws IOException {
        
        checkEnabled();
        
        boolean state = speed > 0 ? true: false;
        switchFan.setState(reverseFan ? !state : state);
    }

    /**
     * {@inheritDoc}
     */
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Single Stage Heatpump Driver (energize to heat)",
                Integer.toHexString(hashCode()),
                "Controls single stage heat pump");
    }

    private void checkEnabled() {
        if (!enabled) {
            throw new IllegalStateException("powerOff() was called already");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void powerOff() {

        NDC.push("powerOff");
        
        try {

            logger.warn("Powering off");

            {
                // won't catch the exception, because if there was a problem shutting down
                // the unit, then better leave the fan on to avoid a complete meltdown
                
                setStage(0);
            }
            
            setFanSpeed(0);
            
            // It is safe to do it here because this method is synchronized
            enabled = false;
        
        } catch (IOException ex) {

            // We're being shut down, and there's a problem??? Better alert the user
            
            logger.fatal("Shutdown failure", ex);
        } finally {
            NDC.pop();
        }
    }
}
