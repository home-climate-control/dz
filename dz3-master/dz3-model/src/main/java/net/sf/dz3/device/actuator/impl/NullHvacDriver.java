package net.sf.dz3.device.actuator.impl;

import java.io.IOException;

import net.sf.dz3.device.model.HvacMode;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Null HVAC driver.
 * 
 * Does nothing except consuming control signals.
 * 
 * Useful for debugging, to avoid stressing your actual hardware.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class NullHvacDriver extends AbstractHvacDriver {
    
    @Override
    protected void doSetMode(HvacMode mode) throws IOException {
        // Do absolutely nothing
    }

    @Override
    protected void doSetStage(int stage) throws IOException {
        // Do absolutely nothing
    }

    @Override
    protected void doSetFanSpeed(double speed) throws IOException {
        // Do absolutely nothing
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Null HVAC Driver",
                Integer.toHexString(hashCode()),
                "Pretends to be the actual HVAC driver");
    }

    @Override
    public void powerOff() {
        
        // Do absolutely nothing
    }
}
