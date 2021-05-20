package net.sf.dz3.controller;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.PidControllerSignalSplitter;
import net.sf.dz3.controller.pid.SimplePidController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class ProcessControllerSplitterTest {
    
    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    public void testSplitterHysteresis() {
        
        ProcessController controller = new HysteresisController(20);
        ProcessControllerSignalSplitter splitter = new ProcessControllerSignalSplitter(controller);
        DataSink<Double> sink = new SimpleSink();
        
        splitter.addConsumer(sink);
        
        DataSample<Double> pv = new DataSample<Double>("source", "sig", 21.0, null);
        controller.compute(pv);
    }

    @Test
    public void testSplitterPidSimple() {
        
        ProcessController controller = new SimplePidController(20, 1, 0, 0, 0);
        ProcessControllerSignalSplitter splitter = new ProcessControllerSignalSplitter(controller);
        DataSink<Double> sink = new SimpleSink();
        
        splitter.addConsumer(sink);
        
        DataSample<Double> pv = new DataSample<Double>("source", "sig", 21.0, null);
        controller.compute(pv);
    }

    @Test
    public void testSplitterPidPid() {
        
        AbstractPidController controller = new SimplePidController(20, 1, 0, 0, 0);
        ProcessControllerSignalSplitter splitter = new PidControllerSignalSplitter(controller);
        DataSink<Double> sink = new SimpleSink();
        
        splitter.addConsumer(sink);
        
        DataSample<Double> pv = new DataSample<Double>("source", "sig", 21.0, null);
        controller.compute(pv);
    }

    private class SimpleSink implements DataSink<Double> {

        @Override
        public void consume(DataSample<Double> signal) {
                
            logger.info("Sample:" + signal);
        }
    }
}
