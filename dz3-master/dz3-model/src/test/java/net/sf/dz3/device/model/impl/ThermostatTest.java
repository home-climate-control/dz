package net.sf.dz3.device.model.impl;

import junit.framework.TestCase;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.impl.NullSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxDescriptor;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * Test cases for {@link ThermostatModel}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2009-2012
 */
public class ThermostatTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Make sure that the thermostat refuses null data sample.
     */
    public void testNull() {

        AnalogSensor sensor = new NullSensor("address", 0);
        AbstractPidController controller = new SimplePidController(20.0, 1.0, 0, 0, 0);
        Thermostat ts = new ThermostatModel("ts",  sensor, controller);

        try {
            ts.consume(null);
        } catch (IllegalArgumentException ex) {
            assertEquals("Wrong exception message", "sample can't be null", ex.getMessage());
        }
    }

    /**
     * Make sure the surrounding logic doesn't break on the thermostat in initial state.
     */
    public void testInitialSignal() {

        AnalogSensor sensor = new NullSensor("address", 0);
        AbstractPidController controller = new SimplePidController(20.0, 1.0, 0, 0, 0);
        Thermostat ts = new ThermostatModel("ts",  sensor, controller);

        ThermostatSignal signal = ts.getSignal();

        assertTrue("Should be error", signal.demand.isError());
        assertEquals("Wrong exception class", IllegalStateException.class, signal.demand.error.getClass());
        assertEquals("Wrong exception message", "No data received yet", signal.demand.error.getMessage());

    }

    /**
     * Make sure that the thermostat doesn't choke on an error signal and properly propagates it
     * to the zone controller.
     * 
     */
    public void testError() throws InterruptedException {

        AnalogSensor sensor = new NullSensor("address", 0);
        AbstractPidController controller = new SimplePidController(20.0, 1.0, 0, 0, 0);
        Thermostat ts = new ThermostatModel("ts",  sensor, controller);

        ZoneController zc = new ThermostateErrorTestZoneController();

        ts.addConsumer(zc);

        DataSample<Double> sample = new DataSample<Double>(System.currentTimeMillis(), null, "sig", null, new Error("Can't read sensor data"));

        assertTrue(sample.isError());

        ts.consume(sample);
    }

    private class ThermostateErrorTestZoneController implements ZoneController {

        @Override
        public void consume(DataSample<ThermostatSignal> signal) {

            logger.info("Signal: " + signal);
        }

        public DataSample<Double> getSignal() {

            throw new UnsupportedOperationException("Not Implemented");
        }

        public void addConsumer(DataSink<Double> consumer) {
            throw new UnsupportedOperationException("Not Implemented");
        }

        public void removeConsumer(DataSink<Double> consumer) {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public JmxDescriptor getJmxDescriptor() {

            throw new UnsupportedOperationException("Not Implemented");
        }
    }

    @SuppressWarnings("unchecked")
    public void testDataSequence() {

        NDC.push("testDataSequence");

        try {

            long timestamp = 0;

            @SuppressWarnings("rawtypes")
            DataSample tempSequence[] = {
                new DataSample<Double>(timestamp++, "source", "signature", 20.0, null),
                new DataSample<Double>(timestamp++, "source", "signature", 20.5, null),
                new DataSample<Double>(timestamp++, "source", "signature", 21.0, null),
                new DataSample<Double>(timestamp++, "source", "signature", 20.5, null),
                new DataSample<Double>(timestamp++, "source", "signature", 20.0, null),
                new DataSample<Double>(timestamp++, "source", "signature", 19.5, null),
                new DataSample<Double>(timestamp++, "source", "signature", 19.0, null)
            };

            AbstractPidController controller = new SimplePidController(20.0, 1.0, 0, 0, 0);
            AnalogSensor sensor = new NullSensor("address", 0);
            Thermostat ts = new ThermostatModel("ts", sensor, controller);

            // Initially, the thermostat is not calling
            assertFalse(ts.getSignal().calling);

            // This is a fake - data sample is injected, but it makes no difference
            ts.consume(tempSequence[0]);

            // still off
            assertFalse(ts.getSignal().calling);
            ts.consume(tempSequence[1]);

            // still off
            assertFalse(ts.getSignal().calling);

            // On now
            logger.info("TURNING ON");
            ts.consume(tempSequence[2]);

            assertTrue(ts.getSignal().calling);

            ts.consume(tempSequence[3]);

            // still on
            assertTrue(ts.getSignal().calling);
            ts.consume(tempSequence[4]);

            // still on
            assertTrue(ts.getSignal().calling);
            ts.consume(tempSequence[5]);
            // still on
            assertTrue(ts.getSignal().calling);
            ts.consume(tempSequence[6]);

            // and off again
            logger.info("TURNING OFF");

            assertFalse(ts.getSignal().calling);

        } finally {
            NDC.pop();
        }
    }
}
