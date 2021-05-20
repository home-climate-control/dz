package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.impl.NullSensor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Test cases for {@link ThermostatModel}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2018
 */
class ThermostatTest {

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Make sure that the thermostat refuses null data sample.
     */
    @Test
    public void testNull() {

        AnalogSensor sensor = new NullSensor("address", 0);
        AbstractPidController controller = new SimplePidController(20.0, 1.0, 0, 0, 0);
        Thermostat ts = new ThermostatModel("ts",  sensor, controller);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> ts.consume(null))
                .withMessage("sample can't be null");
    }

    /**
     * Make sure the surrounding logic doesn't break on the thermostat in initial state.
     */
    @Test
    public void testInitialSignal() {

        AnalogSensor sensor = new NullSensor("address", 0);
        AbstractPidController controller = new SimplePidController(20.0, 1.0, 0, 0, 0);
        Thermostat ts = new ThermostatModel("ts",  sensor, controller);

        ThermostatSignal signal = ts.getSignal();

        assertThat(signal.demand.isError()).isTrue();
        assertThat(signal.demand.error).isInstanceOf(IllegalStateException.class);
        assertThat(signal.demand.error.getMessage()).isEqualTo("No data received yet");
    }

    /**
     * Make sure that the thermostat doesn't choke on an error signal and properly propagates it
     * to the zone controller.
     * 
     */
    @Test
    public void testError() throws InterruptedException {

        AnalogSensor sensor = new NullSensor("address", 0);
        AbstractPidController controller = new SimplePidController(20.0, 1.0, 0, 0, 0);
        Thermostat ts = new ThermostatModel("ts",  sensor, controller);

        ZoneController zc = new ThermostateErrorTestZoneController();

        ts.addConsumer(zc);

        DataSample<Double> sample = new DataSample<Double>(System.currentTimeMillis(), null, "sig", null, new Error("Can't read sensor data"));

        assertThat(sample.isError()).isTrue();

        ts.consume(sample);
    }

    private class ThermostateErrorTestZoneController implements ZoneController {

        @Override
        public void consume(DataSample<ThermostatSignal> signal) {

            logger.info("Signal: " + signal);
        }

        @Override
        public DataSample<Double> getSignal() {

            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public void addConsumer(DataSink<Double> consumer) {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public void removeConsumer(DataSink<Double> consumer) {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public JmxDescriptor getJmxDescriptor() {

            throw new UnsupportedOperationException("Not Implemented");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDataSequence() {

        ThreadContext.push("testDataSequence");

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
            assertThat(ts.getSignal().calling).isFalse();

            // This is a fake - data sample is injected, but it makes no difference
            ts.consume(tempSequence[0]);

            // still off
            assertThat(ts.getSignal().calling).isFalse();
            ts.consume(tempSequence[1]);

            // still off
            assertThat(ts.getSignal().calling).isFalse();

            // On now
            logger.info("TURNING ON");
            ts.consume(tempSequence[2]);

            assertThat(ts.getSignal().calling).isTrue();

            ts.consume(tempSequence[3]);

            // still on
            assertThat(ts.getSignal().calling).isTrue();
            ts.consume(tempSequence[4]);

            // still on
            assertThat(ts.getSignal().calling).isTrue();
            ts.consume(tempSequence[5]);
            // still on
            assertThat(ts.getSignal().calling).isTrue();
            ts.consume(tempSequence[6]);

            // and off again
            logger.info("TURNING OFF");

            assertThat(ts.getSignal().calling).isFalse();

        } finally {
            ThreadContext.pop();
        }
    }
}
