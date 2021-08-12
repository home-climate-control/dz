package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.device.sensor.impl.NullSensor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
class SimpleZoneControllerTest {

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Test the simplest possible combination: {@link SimpleZoneController} with
     * one thermostat using a {@link SimplePidController}.
     */
    @Test
    void test1H() throws InterruptedException {

        ThreadContext.push("test1H");

        try {

            var timestamp = 0L;
            var tempSequence = new LinkedList<DataSample<Double>>();

            tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 20.0, null));
            tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 20.5, null));
            tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 21.0, null));
            tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 20.5, null));
            tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 20.0, null));
            tempSequence.add(new DataSample<Double>(timestamp++, "source", "signature", 19.5, null));
            tempSequence.add(new DataSample<Double>(timestamp,   "source", "signature", 19.0, null));

            var tsSet = new TreeSet<Thermostat>();
            var controller = new SimplePidController(20.0, 1.0, 0, 0, 0);
            var sensor = new NullSensor("address", 0);
            var ts = new ThermostatModel("ts", sensor, controller);
            tsSet.add(ts);

            var zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);

            // Initially, the thermostat is not calling and controller is off
            assertThat(ts.getSignal().calling).isFalse();

            {
                // This is a fake - data sample is injected, but it makes no difference
                ts.consume(tempSequence.remove());

                // still off
                assertThat(ts.getSignal().calling).isFalse();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.0);
            }
            {
                ts.consume(tempSequence.remove());

                // still off
                assertThat(ts.getSignal().calling).isFalse();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.5);
            }

            {
                // On now
                logger.info("TURNING ON");
                ts.consume(tempSequence.remove());

                assertThat(ts.getSignal().calling).isTrue();
                assertThat(ts.getSignal().demand.sample).isEqualTo(2.0);

                // Zone controller should've flipped to on, this is the only thermostat

                assertThat(zc.getSignal().sample).isEqualTo(2.0);
            }
            {
                ts.consume(tempSequence.remove());

                // still on
                assertThat(ts.getSignal().calling).isTrue();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.5);

                assertThat(zc.getSignal().sample).isEqualTo(1.5);
            }
            {
                ts.consume(tempSequence.remove());

                // still on
                assertThat(ts.getSignal().calling).isTrue();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.0);

                assertThat(zc.getSignal().sample).isEqualTo(1.0);
            }
            {
                ts.consume(tempSequence.remove());

                // still on
                assertThat(ts.getSignal().calling).isTrue();
                assertThat(ts.getSignal().demand.sample).isEqualTo(0.5);

                assertThat(zc.getSignal().sample).isEqualTo(0.5);
            }
            {
                // and off again
                logger.info("TURNING OFF");
                ts.consume(tempSequence.remove());


                assertThat(ts.getSignal().calling).isFalse();
                assertThat(ts.getSignal().demand.sample).isEqualTo(0.0);

                assertThat(zc.getSignal().sample).isEqualTo(0.0);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Test the <a href="https://code.google.com/archive/p/diy-zoning/issues/1">"Cold Start" bug</a>.
     *
     * The zone controller should stay off without exceptions when the first ever signal
     * doesn't indicate calling.
     */
    @Test
    void testColdStartNotCalling() {

        ThreadContext.push("testColdStart");

        try {

            var c1 = new SimplePidController(20.0, 1.0, 0, 0, 0);
            var s1 = new NullSensor("address1", 0);
            var t1 = new ThermostatModel("ts1", s1, c1);

            var c2 = new SimplePidController(25.0, 1.0, 0, 0, 0);
            var s2 = new NullSensor("address2", 0);
            var t2 = new ThermostatModel("ts2", s2, c2);

            assertThat(t1.getSignal().calling).isFalse();

            var tsSet = new TreeSet<Thermostat>();

            tsSet.add(t1);
            tsSet.add(t2);

            ZoneController zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);

            {
                t2.consume(new DataSample<Double>(0, "source", "signature", 20.0, null));
                assertThat(t2.getSignal().calling).isFalse();

                var signal = zc.getSignal();

                assertThat(signal).isNotNull();
                assertThat(signal.sample).isEqualTo(0.0);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Test the <a href="https://code.google.com/archive/p/diy-zoning/issues/1">"Cold Start" bug</a>.
     *
     * The zone controller should switch on when the first ever thermostat signal
     * indicates calling.
     */
    @Test
    void testColdStartCalling() {

        ThreadContext.push("testColdStart");

        try {

            var c1 = new SimplePidController(20.0, 1.0, 0, 0, 0);
            var s1 = new NullSensor("address1", 0);
            var t1 = new ThermostatModel("ts1", s1, c1);

            var c2 = new SimplePidController(25.0, 1.0, 0, 0, 0);
            var s2 = new NullSensor("address2", 0);
            var t2 = new ThermostatModel("ts2", s2, c2);

            assertThat(t1.getSignal().calling).isFalse();

            var tsSet = new TreeSet<Thermostat>();

            tsSet.add(t1);
            tsSet.add(t2);

            var zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);

            {
                t2.consume(new DataSample<Double>(0, "source", "signature", 30.0, null));
                assertThat(t2.getSignal().calling).isTrue();

                var signal = zc.getSignal();

                assertThat(signal).isNotNull();
                assertThat(signal.sample).isEqualTo(6.0);
            }

        } finally {
            ThreadContext.pop();
        }
    }
}
