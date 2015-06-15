package net.sf.dz3.device.model.impl;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.impl.NullSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2009-2012
 */
public class SimpleZoneControllerTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Test the simplest possible combination: {@link SimpleZoneController} with
     * one thermostat using a {@link SimplePidController}.
     * @throws InterruptedException 
     */
    public void test1H() throws InterruptedException {

        NDC.push("test1H");

        try {

            long timestamp = 0;

	    Queue<DataSample<Double>> tempSequence = new LinkedList<DataSample<Double>>();
	    
	    tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 20.0, null));
	    tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 20.5, null));
	    tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 21.0, null));
	    tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 20.5, null));
	    tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 20.0, null));
	    tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 19.5, null));
	    tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 19.0, null));

            Set<Thermostat> tsSet = new TreeSet<Thermostat>();
            AbstractPidController controller = new SimplePidController(20.0, 1.0, 0, 0, 0);
            AnalogSensor sensor = new NullSensor("address", 0);
            Thermostat ts = new ThermostatModel("ts", sensor, controller);
            tsSet.add(ts);
            
            ZoneController zc = new SimpleZoneController("zc", tsSet);
            
            logger.info("Zone controller: " + zc);

            // Initially, the thermostat is not calling and controller is off
            assertFalse(ts.getSignal().calling);

            {
                // This is a fake - data sample is injected, but it makes no difference
                ts.consume(tempSequence.remove());

                // still off
                assertFalse(ts.getSignal().calling);
                assertEquals(1.0, ts.getSignal().demand.sample);
            }
            {
                ts.consume(tempSequence.remove());

                // still off
                assertFalse(ts.getSignal().calling);
                assertEquals(1.5, ts.getSignal().demand.sample);
            }

            {
                // On now
                logger.info("TURNING ON");
                ts.consume(tempSequence.remove());

                assertTrue(ts.getSignal().calling);
                assertEquals(2.0, ts.getSignal().demand.sample);

                // Zone controller should've flipped to on, this is the only thermostat

                assertEquals(2.0, zc.getSignal().sample);
            }
            {
                ts.consume(tempSequence.remove());

                // still on
                assertTrue(ts.getSignal().calling);
                assertEquals(1.5, ts.getSignal().demand.sample);

                assertEquals(1.5, zc.getSignal().sample);
            }
            {
                ts.consume(tempSequence.remove());

                // still on
                assertTrue(ts.getSignal().calling);
                assertEquals(1.0, ts.getSignal().demand.sample);

                assertEquals(1.0, zc.getSignal().sample);
            }
            {
                ts.consume(tempSequence.remove());

                // still on
                assertTrue(ts.getSignal().calling);
                assertEquals(0.5, ts.getSignal().demand.sample);

                assertEquals(0.5, zc.getSignal().sample);
            }
            {
                // and off again
                logger.info("TURNING OFF");
                ts.consume(tempSequence.remove());


                assertFalse(ts.getSignal().calling);
                assertEquals(0.0, ts.getSignal().demand.sample);

                assertEquals(0.0, zc.getSignal().sample);
            }

        } finally {
            NDC.pop();
        }
    }
    
    /**
     * Test the "Cold Start" bug ({@link http://code.google.com/p/diy-zoning/issues/detail?id=1}.
     * 
     * The zone controller should stay off without exceptions when the first ever signal.
     */
    public void testColdStartNotCalling() {
        
        NDC.push("testColdStart");
        
        try {
        
            AbstractPidController c1 = new SimplePidController(20.0, 1.0, 0, 0, 0);
            AnalogSensor s1 = new NullSensor("address1", 0);
            Thermostat t1 = new ThermostatModel("ts1", s1, c1);

            AbstractPidController c2 = new SimplePidController(25.0, 1.0, 0, 0, 0);
            AnalogSensor s2 = new NullSensor("address2", 0);
            Thermostat t2 = new ThermostatModel("ts2", s2, c2);

            assertFalse(t1.getSignal().calling);

            Set<Thermostat> tsSet = new TreeSet<Thermostat>();
            
            tsSet.add(t1);
            tsSet.add(t2);
            
            ZoneController zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);
            
            {
                t2.consume(new DataSample<Double>(0, "source", "signature", 20.0, null));
                assertFalse(t2.getSignal().calling);
                
                DataSample<Double> signal = zc.getSignal();

                assertNotNull("Signal can't be null",signal);
                
                assertEquals(0.0, signal.sample);
            }

        } finally {
            NDC.pop();
        }
    }
    /**
     * Test the "Cold Start" bug ({@link http://code.google.com/p/diy-zoning/issues/detail?id=1}.
     * 
     * The zone controller should switch on when the first ever thermostat signal
     * indicates calling.
     */
    public void testColdStartCalling() {
        
        NDC.push("testColdStart");
        
        try {
        
            AbstractPidController c1 = new SimplePidController(20.0, 1.0, 0, 0, 0);
            AnalogSensor s1 = new NullSensor("address1", 0);
            Thermostat t1 = new ThermostatModel("ts1", s1, c1);

            AbstractPidController c2 = new SimplePidController(25.0, 1.0, 0, 0, 0);
            AnalogSensor s2 = new NullSensor("address2", 0);
            Thermostat t2 = new ThermostatModel("ts2", s2, c2);

            assertFalse(t1.getSignal().calling);

            Set<Thermostat> tsSet = new TreeSet<Thermostat>();
            
            tsSet.add(t1);
            tsSet.add(t2);
            
            ZoneController zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);
            
            {
                t2.consume(new DataSample<Double>(0, "source", "signature", 30.0, null));
                assertTrue(t2.getSignal().calling);
                
                DataSample<Double> signal = zc.getSignal();

                assertNotNull("Signal can't be null",signal);
                
                assertEquals(6.0, signal.sample);
            }

        } finally {
            NDC.pop();
        }
    }
}
