package net.sf.dz3.controller;

import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.jukebox.datastream.signal.model.DataSample;
import junit.framework.TestCase;

public class HysteresisControllerTest extends TestCase {
    
    private final Logger logger = Logger.getLogger(getClass());
    private final Random rg = new Random();

    @SuppressWarnings("unchecked")
    public void testController() {
	
	long timestamp = 0;

	@SuppressWarnings("rawtypes")
	DataSample sequence[] = {
    	    new DataSample<Double>(timestamp++, "source", "signature", 20.0, null),
    	    new DataSample<Double>(timestamp++, "source", "signature", 20.5, null),
    	    new DataSample<Double>(timestamp++, "source", "signature", 21.0, null),
    	    new DataSample<Double>(timestamp++, "source", "signature", 20.5, null),
    	    new DataSample<Double>(timestamp++, "source", "signature", 20.0, null),
    	    new DataSample<Double>(timestamp++, "source", "signature", 19.5, null),
    	    new DataSample<Double>(timestamp++, "source", "signature", 19.0, null)
    	    };
	
	ProcessController pc = new HysteresisController(20);
	
	assertEquals(-1.0, pc.compute(sequence[0]).sample);
	assertEquals(-1.0, pc.compute(sequence[1]).sample);
	assertEquals(1.0, pc.compute(sequence[2]).sample);
	assertEquals(1.0, pc.compute(sequence[3]).sample);
	assertEquals(1.0, pc.compute(sequence[4]).sample);
	assertEquals(1.0, pc.compute(sequence[5]).sample);
	assertEquals(-1.0, pc.compute(sequence[6]).sample);
	
    }
    
    /**
     * Make sure that the signal timestamp is the same as input sample timestamp.
     */
    public void testTimestamp() {
	
	NDC.push("testTimestamp");
	
	try {
	    
	    long timestamp = System.currentTimeMillis() + rg.nextInt();

	    logger.info("Original timestamp: " + timestamp);

	    DataSample<Double> pv = new DataSample<Double>(timestamp, "source", "signature", 0.0, null);
	    ProcessController pc = new HysteresisController(0);
	    DataSample<Double> signal = pc.compute(pv);

	    logger.info("Sample: " + pv);
	    logger.info("Signal: " + signal);
	    
	    assertEquals(timestamp, signal.timestamp);
	
	} finally {
	    NDC.pop();
	}
    }
    
    public void testNullPV() {

        HysteresisController pc = new HysteresisController(0);
        
        // This must not blow up like it did before
        pc.compute();
    }
}
