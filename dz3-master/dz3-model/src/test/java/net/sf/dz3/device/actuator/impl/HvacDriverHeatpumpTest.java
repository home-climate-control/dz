package net.sf.dz3.device.actuator.impl;

import java.io.IOException;

import junit.framework.TestCase;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.sensor.impl.NullSwitch;

public class HvacDriverHeatpumpTest extends TestCase {
    
    /**
     * See {@link https://github.com/home-climate-control/dz/issues/3} for details.
     */
    public void testRunaway() throws IOException {
        
        NullSwitch switchMode = new NullSwitch("mode");
        NullSwitch switchRunning = new NullSwitch("running");
        NullSwitch switchFan = new NullSwitch("fan");
        
        HvacDriverHeatpump driver = new HvacDriverHeatpump(switchMode, switchRunning, switchFan);
        
        {
            // Change the mode and verify that it worked

            assertEquals("Wrong mode before the switch", false, switchMode.getState());
            driver.setMode(HvacMode.HEATING);
            assertEquals("Wrong mode after the switch", true, switchMode.getState());
        }

        {
            // Normal cycle
            
            driver.setFanSpeed(1);
            driver.setStage(1);
            
            double[] fanSpeed = driver.getFanSpeed();
            int[] stage = driver.getStage();
            
            assertEquals("Wrong expected fan speed", 1.0, fanSpeed[0]);
            assertEquals("Wrong actual fan speed", 1.0, fanSpeed[1]);
            assertEquals("Wrong fan speed switch state", true, switchFan.getState());
            assertEquals("Wrong expected stage", 1, stage[0]);
            assertEquals("Wrong actual stage", 1, stage[1]);
            assertEquals("Wrong stage switch state", true, switchFan.getState());
            
            assertEquals("Wrong mode after the normal cycle", true, switchMode.getState());
        }

        {
            // Boom! We lose power.
            
            switchMode.setState(false);
        }
        
        {
            // Switch off after power loss

            driver.setFanSpeed(0);
            driver.setStage(0);

            double[] fanSpeed = driver.getFanSpeed();
            int[] stage = driver.getStage();
            
            assertEquals("Wrong expected fan speed", 0.0, fanSpeed[0]);
            assertEquals("Wrong actual fan speed", 0.0, fanSpeed[1]);
            assertEquals("Wrong fan speed switch state", false, switchFan.getState());
            assertEquals("Wrong expected stage", 0, stage[0]);
            assertEquals("Wrong actual stage", 0, stage[1]);
            assertEquals("Wrong stage switch state", false, switchFan.getState());
            
            // Not checking this switch now, irrelevant 
            // assertEquals("Wrong mode after the normal cycle", true, switchMode.getState());
        }
        
        {
            // Cycle after power loss.
            
            // All the switches are in their default ("failsafe") positions. We don't care about the fan speed
            // and the stage at this point because they will be forcibly set up to desired value anyway,
            // but we *do* care about the mode switch being in the proper position. If it is not,
            // we get a positive feedback runaway loop - at best, we'll some money and some comfort,
            // at worst, catastrophic meltdown - the system will just keep running until manually interrupted.
            
            // Let's make sure the switch is still in its "power loss" state
            
            assertEquals("Wrong 'power loss' mode switch state", false, switchMode.getState());

            
            driver.setFanSpeed(1);
            driver.setStage(1);
            
            double[] fanSpeed = driver.getFanSpeed();
            int[] stage = driver.getStage();
            
            assertEquals("Wrong expected fan speed", 1.0, fanSpeed[0]);
            assertEquals("Wrong actual fan speed", 1.0, fanSpeed[1]);
            assertEquals("Wrong expected stage", 1, stage[0]);
            assertEquals("Wrong actual stage", 1, stage[1]);
            
            assertEquals("Wrong mode after the power loss cycle", true, switchMode.getState());
        }
    }
}
