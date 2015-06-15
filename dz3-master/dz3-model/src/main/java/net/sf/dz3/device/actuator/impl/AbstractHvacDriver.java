package net.sf.dz3.device.actuator.impl;

import java.io.IOException;

import net.sf.dz3.device.actuator.HvacDriver;
import net.sf.dz3.device.model.HvacMode;
import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.jukebox.logger.LogAware;

import org.apache.log4j.NDC;

/**
 * Abstract HVAC driver.
 * 
 * Provides common logic for state housekeeping and JMX reporting.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public abstract class AbstractHvacDriver extends LogAware implements HvacDriver {

    private HvacState expected;
    private HvacState actual;
    
    public AbstractHvacDriver() {
    
        expected = new HvacState();
        actual = new HvacState();
    }
    
    @Override
    public final double[] getFanSpeed() {
        return new double[] {expected.speed, actual.speed};
    }
    
    @Override
    public final HvacMode[] getMode() {
        return new HvacMode[] {expected.mode, actual.mode};
    }

    @Override
    public final int[] getStage() {
        return new int[] {expected.stage, actual.stage};
    }
    
    @Override
    public final void setMode(HvacMode mode) throws IOException {
        
        NDC.push("setMode");
        
        try {
            
            logger.info("mode=" + mode);
            expected.mode = mode;
            
            doSetMode(mode);
            
            actual.mode = mode;
            
        } finally {
            NDC.pop();
        }
    }

    @Override
    public final void setStage(int stage) throws IOException {
        
        NDC.push("setStage");
        
        try {
            
            logger.info("stage=" + stage);
            expected.stage = stage;
            
            doSetStage(stage);
            
            actual.stage = stage;
            
        } finally {
            NDC.pop();
        }
    }

    @Override
    public final void setFanSpeed(double speed) throws IOException {
        
        NDC.push("setFanSpeed");
        
        try {
            
            logger.info("fanSpeed=" + speed);
            expected.speed = speed;
            
            doSetFanSpeed(speed);
            
            actual.speed = speed;
            
        } finally {
            NDC.pop();
        }
    }

    protected abstract  void doSetMode(HvacMode mode) throws IOException;

    protected abstract void doSetStage(int stage) throws IOException;

    protected abstract void doSetFanSpeed(double speed) throws IOException;

    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Null HVAC Driver",
                Integer.toHexString(hashCode()),
                "Pretends to be the actual HVAC driver");
    }
    
    protected static class HvacState {
        
        public HvacMode mode;
        public int stage;
        public double speed;
        
        public HvacState() {
            
            mode = HvacMode.OFF;
            stage = 0;
            speed = 0;
        }
    }
}
