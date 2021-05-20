package net.sf.dz3.controller;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class HysteresisControllerTest {
    
    private final Logger logger = LogManager.getLogger(getClass());
    private final Random rg = new Random();

    @SuppressWarnings("unchecked")
	@Test
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

		assertThat(pc.compute(sequence[0]).sample).isEqualTo(-1.0);
		assertThat(pc.compute(sequence[1]).sample).isEqualTo(-1.0);
		assertThat(pc.compute(sequence[2]).sample).isEqualTo(1.0);
		assertThat(pc.compute(sequence[3]).sample).isEqualTo(1.0);
		assertThat(pc.compute(sequence[4]).sample).isEqualTo(1.0);
		assertThat(pc.compute(sequence[5]).sample).isEqualTo(1.0);
		assertThat(pc.compute(sequence[6]).sample).isEqualTo(-1.0);
    }
    
    /**
     * Make sure that the signal timestamp is the same as input sample timestamp.
     */
    public void testTimestamp() {
	
	ThreadContext.push("testTimestamp");
	
	try {
	    
	    long timestamp = System.currentTimeMillis() + rg.nextInt();

	    logger.info("Original timestamp: " + timestamp);

	    DataSample<Double> pv = new DataSample<Double>(timestamp, "source", "signature", 0.0, null);
	    ProcessController pc = new HysteresisController(0);
	    DataSample<Double> signal = pc.compute(pv);

	    logger.info("Sample: " + pv);
	    logger.info("Signal: " + signal);
	    
	    assertThat(signal.timestamp).isEqualTo(timestamp);
	
	} finally {
	    ThreadContext.pop();
	}
    }
    
    public void testNullPV() {

        HysteresisController pc = new HysteresisController(0);
        
        // This must not blow up like it did before
        pc.compute();
    }
}
