package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.sensor.impl.NullSensor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class ThermostatSplitterTest {
    
    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    public void testSplitterHysteresis() {

        var ts = new ThermostatModel("tsName", new NullSensor("sensor", 1000), new SimplePidController(20, 1, 0, 0, 0));
        var splitter = new ThermostatSignalSplitter(ts);
        var sink = new SimpleSink();

        splitter.addConsumer(sink);

        var pv = new DataSample<Double>("source", "sig", 21.0, null);
        
        ts.consume(pv);
    }

    private class SimpleSink implements DataSink<Double> {

        @Override
        public void consume(DataSample<Double> signal) {

            logger.info("Sample:" + signal);
        }

    }
}
