package net.sf.dz3.view.swing.thermostat;

import java.awt.Dimension;
import java.awt.event.KeyEvent;

import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.swing.ScreenDescriptor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxDescriptor;
import junit.framework.TestCase;

public class ThermostatPanelTest extends TestCase {

    public void testNullScheduler() {

        ThermostatModel tm = new ThermostatModel("model", new MockSensor(), new SimplePidController(1, 1, 1, 1, 1));
        ThermostatPanel tp = new ThermostatPanel(tm, new ScreenDescriptor("", new Dimension(0, 0), null, null, null), null);
        
        tp.keyPressed(new KeyEvent(tp, 0, 0, 0, 0, 's', 0));
    }
    
    private class MockSensor implements AnalogSensor {
        
        
        @Override
        public String getAddress() {
            throw new IllegalStateException("Not Implemented");
        }
        
        @Override
        public JmxDescriptor getJmxDescriptor() {
            throw new IllegalStateException("Not Implemented");
        }
        
        @Override
        public void removeConsumer(DataSink<Double> arg0) {
            throw new IllegalStateException("Not Implemented");
        }
        
        @Override
        public void addConsumer(DataSink<Double> arg0) {
            // Do nothing
        }
        
        @Override
        public DataSample<Double> getSignal() {
            throw new IllegalStateException("Not Implemented");
        }
    }
}
