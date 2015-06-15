package net.sf.dz3.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.PID_Controller;
import net.sf.dz3.controller.pid.PidControllerStatus;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.jukebox.datastream.signal.model.DataSample;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;


public class PID_ControllerTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());
    private final Random rg = new Random();

    public void testPSimple() {
	testP(new SimplePidController(0, 1, 0, 0, 0));
    }
    
    public void testPStateful() {
	testP(new PID_Controller(0.0, 1, 0.0, 1, 0.0, 1, 0));
    }

    /**
     * Test the proportional channel.
     */
    public void testP(ProcessController pc) {
	
	NDC.push("testP/" + pc.getClass().getName());
	
	try {

	    // Feel free to push this north of 5000000
	    long COUNT = 10000;
	    long start = System.currentTimeMillis();

	    for (int count = 0; count < COUNT; count++) {

		double value = rg.nextDouble();
		// Time is relevant, timestamp can't be the same or go back in time
		DataSample<Double> pv = new DataSample<Double>(count, "source", "signature", value, null);
		DataSample<Double> signal = pc.compute(pv);

		assertEquals(value, signal.sample);
	    }

	    long now = System.currentTimeMillis();

	    double rqPerSec = (double) (COUNT * 1000L) / (double) (now - start);

	    logger.info((now - start) + "ms");
	    logger.info(rqPerSec +" requests per second");
	    
	} finally {
	    NDC.pop();
	}
    }
    
    /**
     * Make sure the PI behavior of stateful and stateless controllers
     * is identical.
     */
    public void testReconcile() {

	NDC.push("testReconcile");

	try {

	    double P = 1;
	    double I = 0.01;
	    long Ispan = 1000;
	    int divider = 10;
	    long start = System.currentTimeMillis();
	    long timestamp = start;
	    double delta = 0.1;

	    List<DataSample<Double>> data = new ArrayList<DataSample<Double>>();

            // Make sure the test timespan doesn't go beyond Ispan,
            // stateful behavior is different then
            for (int count = 0; count < divider - 1; count++) {

		data.add(new DataSample<Double>(timestamp, "source", "signature", delta, null));
		timestamp += Ispan/divider;
	    }

            ProcessController pcStateless = new SimplePidController(0, P, I, 0, 0);
	    ProcessController pcStateful = new PID_Controller(0.0, P, I, Ispan, 0.0, 1, 0);

	    for (Iterator<DataSample<Double>> i = data.iterator(); i.hasNext(); ) {

		DataSample<Double> pv = i.next();
		DataSample<Double> signalStateless = pcStateless.compute(pv);
		DataSample<Double> signalStateful = pcStateful.compute(pv);

		long offset = signalStateless.timestamp - start;
		logger.debug("signal: " + signalStateless.sample + "/" + signalStateful.sample + "@" + offset);

                if (offset > Ispan) {
		    fail("offset beyond Ispan: " + offset);
		}

                assertEquals(signalStateless.timestamp, signalStateful.timestamp);
                assertEquals(signalStateless.sample, signalStateful.sample);

                assertEquals(I * offset * delta + delta, signalStateful.sample.doubleValue(), 0.00001);
	    }

	} finally {
	    NDC.pop();
	}
    }

    /**
     * Test the integral component.
     */
    public void testI() {
	
	NDC.push("testI");
	
	try {
	    
	    double P = 1;
	    double I = 0.01;
	    long Ispan = 1000;
	    int divider = 10;
	    long start = System.currentTimeMillis();
	    long timestamp = start;
	    double delta = 0.1;
	    double limit = I * Ispan * delta + delta;
	    
	    List<DataSample<Double>> data = new ArrayList<DataSample<Double>>();
	    
	    for (int count = 0; count < divider * 2; count++) {
		data.add(new DataSample<Double>(timestamp, "source", "signature", delta, null));
		timestamp += Ispan/divider;
	    }
	    
	    ProcessController pc = new PID_Controller(0.0, P, I, Ispan, 0.0, 1, 0);
	    
	    for (Iterator<DataSample<Double>> i = data.iterator(); i.hasNext(); ) {
		
		logger.debug("sample: " + i.next());
	    }

	    for (Iterator<DataSample<Double>> i = data.iterator(); i.hasNext(); ) {
		
		DataSample<Double> pv = i.next();
		DataSample<Double> signal = pc.compute(pv);

		long offset = signal.timestamp - start;
		logger.debug("signal: " + signal + " @" + offset);
		
		if (offset < Ispan) {
		    assertEquals(I * offset * delta + delta, signal.sample, 0.00001);
		} else {
		    assertEquals(limit, signal.sample);
		}
	    }

	} finally {
	    NDC.pop();
	}
    }
    
    /**
     * Estimate how bad the performance of the integral set is, in real life.
     */
    public void testIrateSimple() {

        double P = 1;
        double I = 0.0001;
        long Ispan = 15 * 60 * 1000;
        int deltaT = 300;
        long start = System.currentTimeMillis();
        long COUNT = (Ispan / deltaT) * 2;

        ProcessController pc = new SimplePidController(0.0, P, I, 0, 0);
        testIrate(pc, start, deltaT, COUNT);
    }

    /**
     * Estimate how bad the performance of the integral set is, in real life.
     */
    public void testIrateStateful() {

        double P = 1;
        double I = 0.0001;
        long Ispan = 15 * 60 * 1000;
        int deltaT = 300;
        long start = System.currentTimeMillis();
        long COUNT = (Ispan / deltaT) * 2;

        ProcessController pc = new PID_Controller(0.0, P, I, Ispan, 0.0, 1, 0);
        testIrate(pc, start, deltaT, COUNT);
    }
    /**
     * Estimate how bad the performance of the integral set is, in real life.
     */
    public void testIrate(ProcessController pc, long start, int deltaT, long COUNT) {
	
	NDC.push("testIrate/" + pc.getClass().getName());
	
	try {
	    
	    long timestamp = start;

	    logger.info("Count: " + COUNT);

	    for (int count = 0; count < COUNT; count++) {

		DataSample<Double> pv = new DataSample<Double>(
			timestamp,
			"source",
			"signature",
			rg.nextDouble(),
			null);
		
		pc.compute(pv);
		timestamp += rg.nextInt(deltaT) + 1;
	    }
	    
	    long now = System.currentTimeMillis();
	    double rqPerSec = (double) (COUNT * 1000L) / (double) (now - start);

	    logger.info((now - start) + "ms");
	    logger.info(rqPerSec +" requests per second");
	
	} finally {
	    NDC.pop();
	}
    }

    /**
     * Test the derivative component.
     */
    public void testD() {
	
	NDC.push("testD");
	
	try {
	    
	    double P = 1;
	    double D = 1000;
	    long Dspan = 1000;
	    int divider = 10;
	    long start = System.currentTimeMillis();
	    long timestamp = start;
	    
	    List<DataSample<Double>> data = new ArrayList<DataSample<Double>>();
	    
	    for (int count = 0; count < divider * 2; count++) {
		data.add(new DataSample<Double>(
			timestamp,
			"source",
			"signature",
			timestamp == start ? 0.0 : 1.0,
			null));
		timestamp += Dspan/divider;
	    }
	    
	    ProcessController pc = new PID_Controller(0.0, P, 0, 1, D, Dspan, 0);
	    
	    for (Iterator<DataSample<Double>> i = data.iterator(); i.hasNext(); ) {
		
		logger.debug("sample: " + i.next());
	    }

	    for (Iterator<DataSample<Double>> i = data.iterator(); i.hasNext(); ) {
		
		DataSample<Double> pv = i.next();
		DataSample<Double> signal = pc.compute(pv);

		long offset = signal.timestamp - start;
		logger.debug("signal: " + signal + " @" + offset);
	    }

	} finally {
	    NDC.pop();
	}
    }
    
    public void testSimplePidControllerID() {

        testPidControllerID(new SimplePidController(20, 1, 0, 0, 0), "simple");
    }
    
    public void testTimedPidControllerID() {

        testPidControllerID(new PID_Controller(20, 1.0, 0.0, 1000, 0.0, 1000, 0), "timed");
    }

    public void testPidControllerID(AbstractPidController controller, String ndc) {
        
        NDC.push("testSimplePidControllerID");
        
        try {
        
            for (long timestamp = 0; timestamp < 100; timestamp++) {

                DataSample<Double> pv = new DataSample<Double>(timestamp, "source", "sig", 21.0 + rg.nextInt(10), null);
                controller.compute(pv);

                PidControllerStatus signal = (PidControllerStatus) controller.getStatus();
                logger.info(signal);

                assertEquals(0.0, signal.i, 0.000000000000001);
                assertEquals(0.0, signal.d, 0.000000000000001);
            }
            
        } finally {
            NDC.pop();
        }
    }
    
    public void testSaturationSimple() {
     
        testSaturationNull(new SimplePidController(20, 1, 1, 0, 3), "simple");
    }
    
    public void testSaturationNullTimed() {

        testSaturationNull(new PID_Controller(20, 1.0, 1, 1000, 0.0, 1000, 3), "timed");
    }
    
    public void testSaturationNull(AbstractPidController controller, String ndc) {
        
        NDC.push("testSimplePidControllerID");
        
        try {
        
            for (long timestamp = 0; timestamp < 100; timestamp++) {

                DataSample<Double> pv = new DataSample<Double>(timestamp, "source", "sig", 21.0, null);
                controller.compute(pv);

                PidControllerStatus signal = (PidControllerStatus) controller.getStatus();
                logger.info(signal);

                assertEquals(0.0, signal.d, 0.000000000000001);
            }
            
        } finally {
            NDC.pop();
        }
    }
   
}
