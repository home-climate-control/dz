package net.sf.dz3.device.model.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import junit.framework.TestCase;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.sensor.impl.NullSensor;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;

public class ThermostatSplitterTest extends TestCase {
    
    private final Logger logger = LogManager.getLogger(getClass());

    public void testSplitterHysteresis() {

        Thermostat ts = new ThermostatModel("tsName", new NullSensor("sensor", 1000), new SimplePidController(20, 1, 0, 0, 0));
        ThermostatSignalSplitter splitter = new ThermostatSignalSplitter(ts);
        DataSink<Double> sink = new SimpleSink();

        splitter.addConsumer(sink);

        DataSample<Double> pv = new DataSample<Double>("source", "sig", 21.0, null);
        
        ts.consume(pv);
    }

    private class SimpleSink implements DataSink<Double> {

        public void consume(DataSample<Double> signal) {

            logger.info("Sample:" + signal);
        }

    }
}
