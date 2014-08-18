package net.sf.dz3.controller;

import org.apache.log4j.Logger;

import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.PidControllerSignalSplitter;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import junit.framework.TestCase;

public class ProcessControllerSplitterTest extends TestCase {
    
    private final Logger logger = Logger.getLogger(getClass());
    
    public void testSplitterHysteresis() {
        
        ProcessController controller = new HysteresisController(20);
        ProcessControllerSignalSplitter splitter = new ProcessControllerSignalSplitter(controller);
        DataSink<Double> sink = new SimpleSink();
        
        splitter.addConsumer(sink);
        
        DataSample<Double> pv = new DataSample<Double>("source", "sig", 21.0, null);
        controller.compute(pv);
    }
    
    public void testSplitterPidSimple() {
        
        ProcessController controller = new SimplePidController(20, 1, 0, 0, 0);
        ProcessControllerSignalSplitter splitter = new ProcessControllerSignalSplitter(controller);
        DataSink<Double> sink = new SimpleSink();
        
        splitter.addConsumer(sink);
        
        DataSample<Double> pv = new DataSample<Double>("source", "sig", 21.0, null);
        controller.compute(pv);
    }

    public void testSplitterPidPid() {
        
        AbstractPidController controller = new SimplePidController(20, 1, 0, 0, 0);
        ProcessControllerSignalSplitter splitter = new PidControllerSignalSplitter(controller);
        DataSink<Double> sink = new SimpleSink();
        
        splitter.addConsumer(sink);
        
        DataSample<Double> pv = new DataSample<Double>("source", "sig", 21.0, null);
        controller.compute(pv);
    }

    private class SimpleSink implements DataSink<Double> {

        public void consume(DataSample<Double> signal) {
                
            logger.info("Sample:" + signal);
        }
        
    }
}
