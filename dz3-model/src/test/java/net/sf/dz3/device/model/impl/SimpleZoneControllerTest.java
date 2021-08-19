package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
class SimpleZoneControllerTest {

    private final Logger logger = LogManager.getLogger(getClass());

    private LinkedList<DataSample<Double>> createSequence() {

        var timestamp = 0L;
        return new LinkedList<>(List.of(
                new DataSample<Double>(timestamp++, "source", "signature", 20.0, null),
                new DataSample<Double>(timestamp++, "source", "signature", 20.5, null),
                new DataSample<Double>(timestamp++, "source", "signature", 21.0, null),
                new DataSample<Double>(timestamp++, "source", "signature", 20.5, null),
                new DataSample<Double>(timestamp++, "source", "signature", 20.0, null),
                new DataSample<Double>(timestamp++, "source", "signature", 19.5, null),
                new DataSample<Double>(timestamp,   "source", "signature", 19.0, null)));
    }

    /**
     * Test the P controller from {@link #testOneZone()}.
     */
    @Test
    void testOneP() {

        ThreadContext.push("testOneP");

        try {

            var controller = new SimplePidController("simple20", 20.0, 1.0, 0, 0, 0);

            logger.info("initial status: {}", controller.getStatus());

            var tempSequence = createSequence();

            controller.consume(tempSequence.remove()); // 20.0
            assertThat(controller.getStatus().signal.sample).isZero();

            controller.consume(tempSequence.remove()); // 20.5
            assertThat(controller.getStatus().signal.sample).isEqualTo(0.5);

            controller.consume(tempSequence.remove()); // 21.0
            assertThat(controller.getStatus().signal.sample).isEqualTo(1.0);

            controller.consume(tempSequence.remove()); // 20.5
            assertThat(controller.getStatus().signal.sample).isEqualTo(0.5);

            controller.consume(tempSequence.remove()); // 20.0
            assertThat(controller.getStatus().signal.sample).isZero();

            controller.consume(tempSequence.remove()); // 19.5
            assertThat(controller.getStatus().signal.sample).isEqualTo(-0.5);

            controller.consume(tempSequence.remove()); // 19.0
            assertThat(controller.getStatus().signal.sample).isEqualTo(-1.0);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Test the thermostat from {@link #testOneZone()}.
     */
    @Test
    void testOneThermostat() {

        ThreadContext.push("testOneP");

        try {

            var controller = new SimplePidController("simple20", 20.0, 1.0, 0, 0, 0);
            var ts = new ThermostatModel("ts", mock(AnalogSensor.class), controller);

            logger.info("initial status: {}", controller.getStatus());

            var tempSequence = createSequence();

            // Initially, the thermostat is not calling and controller is off
            assertThat(ts.getSignal().calling).isFalse();

            {
                ts.consume(tempSequence.remove()); // 20.0

                // still off
                assertThat(ts.getSignal().calling).isFalse();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.0);
            }
            {
                ts.consume(tempSequence.remove()); // 20.5

                // still off
                assertThat(ts.getSignal().calling).isFalse();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.5);
            }

            {
                // On now
                logger.info("TURNING ON");
                ts.consume(tempSequence.remove()); // 21.0

                assertThat(ts.getSignal().calling).isTrue();
                assertThat(ts.getSignal().demand.sample).isEqualTo(2.0);
            }
            {
                ts.consume(tempSequence.remove()); // 20.5

                // still on
                assertThat(ts.getSignal().calling).isTrue();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.5);
            }
            {
                ts.consume(tempSequence.remove()); // 20.0

                // still on
                assertThat(ts.getSignal().calling).isTrue();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.0);
            }
            {
                ts.consume(tempSequence.remove()); // 19.5

                // still on
                assertThat(ts.getSignal().calling).isTrue();
                assertThat(ts.getSignal().demand.sample).isEqualTo(0.5);
            }
            {
                // and off again
                logger.info("TURNING OFF");
                ts.consume(tempSequence.remove()); // 19.0


                assertThat(ts.getSignal().calling).isFalse();
                assertThat(ts.getSignal().demand.sample).isEqualTo(0.0);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Test the simplest possible combination: {@link SimpleZoneController} with
     * one thermostat using a {@link SimplePidController}.
     */
    @Test
    void testOneZone() throws InterruptedException {

        ThreadContext.push("testOneZone");

        try {

            var tempSequence = createSequence();

            var tsSet = new TreeSet<Thermostat>();
            var controller = new SimplePidController("simple20", 20.0, 1.0, 0, 0, 0);
            var ts = new ThermostatModel("ts", mock(AnalogSensor.class), controller);
            tsSet.add(ts);

            var zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);

            // Initially, the thermostat is not calling and controller is off
            assertThat(ts.getSignal().calling).isFalse();

            {
                ts.consume(tempSequence.remove());

                // still off
                assertThat(ts.getSignal().calling).isFalse();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.0);
                assertThat(zc.getSignal().sample).isEqualTo(0.0);
            }
            {
                ts.consume(tempSequence.remove());

                // still off
                assertThat(ts.getSignal().calling).isFalse();
                assertThat(ts.getSignal().demand.sample).isEqualTo(1.5);
                assertThat(zc.getSignal().sample).isEqualTo(0.0);
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

            var c1 = new SimplePidController("simple20", 20.0, 1.0, 0, 0, 0);
            var t1 = new ThermostatModel("ts1", mock(AnalogSensor.class), c1);

            var c2 = new SimplePidController("simple25", 25.0, 1.0, 0, 0, 0);
            var t2 = new ThermostatModel("ts2", mock(AnalogSensor.class), c2);

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

            var c1 = new SimplePidController("simple20", 20.0, 1.0, 0, 0, 0);
            var t1 = new ThermostatModel("ts1", mock(AnalogSensor.class), c1);

            var c2 = new SimplePidController("simple25", 25.0, 1.0, 0, 0, 0);
            var t2 = new ThermostatModel("ts2", mock(AnalogSensor.class), c2);

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
    /**
     * Make sure non-voting zones don't start the HVAC.
     */
    @Test
    void nonVoting() {

        ThreadContext.push("nonVoting");

        try {

            var c1 = new SimplePidController("simple20", 20.0, 1.0, 0, 0, 0);
            var t1 = new ThermostatModel("ts1", mock(AnalogSensor.class), c1);
            t1.setVoting(false);

            var c2 = new SimplePidController("simple25", 25.0, 1.0, 0, 0, 0);
            var t2 = new ThermostatModel("ts2", mock(AnalogSensor.class), c2);

            var tsSet = new TreeSet<Thermostat>();

            tsSet.add(t1);
            tsSet.add(t2);

            var zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);

            var now = Instant.now();
            var s23 = new DataSample<>(now.toEpochMilli(), "source", "signature", 23.0, null);
            var s28 = new DataSample<>(now.plus(10, ChronoUnit.SECONDS).toEpochMilli(), "source", "signature", 28.0, null);

            {
                t1.consume(s23);
                assertThat(t1.getSignal().calling).isTrue();

                t2.consume(s23);
                assertThat(t2.getSignal().calling).isFalse();

                var signal = zc.getSignal();

                assertThat(signal).isNotNull();
                assertThat(signal.sample).isEqualTo(0.0);
            }

            {
                t1.consume(s28);
                assertThat(t1.getSignal().calling).isTrue();

                t2.consume(s28);
                assertThat(t2.getSignal().calling).isTrue();

                var signal = zc.getSignal();

                assertThat(signal).isNotNull();
                assertThat(signal.sample).isEqualTo(13.0);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure the <a href="https://github.com/home-climate-control/dz/issues/195">last enabled zone's voting status is ignored</a>,
     * for the case when it is the last enabled zone of many.
     */
    @Test
    void lastZoneOfManyNonVoting() {

        ThreadContext.push("lastZoneOfManyNonVoting");

        try {

            var c1 = new SimplePidController("simple20", 20.0, 1.0, 0, 0, 0);
            var t1 = new ThermostatModel("ts1", mock(AnalogSensor.class), c1);
            t1.setVoting(false);

            var c2 = new SimplePidController("simple25", 25.0, 1.0, 0, 0, 0);
            var t2 = new ThermostatModel("ts2", mock(AnalogSensor.class), c2);
            t2.setOn(false);

            var tsSet = new TreeSet<Thermostat>();

            tsSet.add(t1);
            tsSet.add(t2);

            var zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);

            var now = Instant.now();
            var s23 = new DataSample<>(now.toEpochMilli(), "source", "signature", 23.0, null);

            {
                t1.consume(s23);
                assertThat(t1.getSignal().calling).isTrue();

                t2.consume(s23);
                assertThat(t2.getSignal().calling).isFalse();

                var signal = zc.getSignal();

                assertThat(signal).isNotNull();
                assertThat(signal.sample).isEqualTo(4.0);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure the <a href="https://github.com/home-climate-control/dz/issues/195">last enabled zone's voting status is ignored</a>,
     * for the case when it is the only zone configured for the zone controller.
     */
    @Test
    void onlyZoneNonVoting() {

        ThreadContext.push("onlyZoneNonVoting");

        try {

            var c1 = new SimplePidController("simple20", 20.0, 1.0, 0, 0, 0);
            var t1 = new ThermostatModel("ts1", mock(AnalogSensor.class), c1);
            t1.setVoting(false);

            var tsSet = new TreeSet<Thermostat>();

            tsSet.add(t1);

            var zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);

            var now = Instant.now();
            var s23 = new DataSample<>(now.toEpochMilli(), "source", "signature", 23.0, null);

            {
                t1.consume(s23);
                assertThat(t1.getSignal().calling).isTrue();

                var signal = zc.getSignal();

                assertThat(signal).isNotNull();
                assertThat(signal.sample).isEqualTo(4.0);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure disabled thermostats don't start the HVAC unit.
     */
    @Test
    void disabled() {

        ThreadContext.push("disabled");

        try {

            var c1 = new SimplePidController("simple20", 20.0, 1.0, 0, 0, 0);
            var t1 = new ThermostatModel("ts1", mock(AnalogSensor.class), c1);
            t1.setOn(false);

            var tsSet = new TreeSet<Thermostat>();

            tsSet.add(t1);

            var zc = new SimpleZoneController("zc", tsSet);

            logger.info("Zone controller: " + zc);

            var now = Instant.now();
            var s23 = new DataSample<>(now.toEpochMilli(), "source", "signature", 23.0, null);

            {
                t1.consume(s23);
                assertThat(t1.getSignal().calling).isFalse();

                var signal = zc.getSignal();

                assertThat(signal).isNotNull();
                assertThat(signal.sample).isEqualTo(0.0);
            }

        } finally {
            ThreadContext.pop();
        }
    }
}
