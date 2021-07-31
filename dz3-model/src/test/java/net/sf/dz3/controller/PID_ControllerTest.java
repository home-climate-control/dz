package net.sf.dz3.controller;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxWrapper;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.PID_Controller;
import net.sf.dz3.controller.pid.PidControllerStatus;
import net.sf.dz3.controller.pid.SimplePidController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PID_ControllerTest {

    private final Logger logger = LogManager.getLogger(getClass());
    private final Random rg = new Random();

    @Test
    void testPSimple() {
        testP(new SimplePidController(0, 1, 0, 0, 0));
    }

    @Test
    void testPStateful() {
        testP(new PID_Controller(0.0, 1, 0.0, 1, 0.0, 1, 0));
    }

    /**
     * Test the proportional channel.
     */
    private void testP(ProcessController pc) {

        ThreadContext.push("testP/" + pc.getClass().getName());

        try {

            // Feel free to push this north of 5000000
            var COUNT = 10000L;
            var start = System.currentTimeMillis();

            for (var count = 0; count < COUNT; count++) {

                var value = rg.nextDouble();
                // Time is relevant, timestamp can't be the same or go back in time
                var pv = new DataSample<>(count, "source", "signature", value, null);
                var signal = pc.compute(pv);

                assertThat(signal.sample).isEqualTo(value);
            }

            var now = System.currentTimeMillis();

            var rqPerSec = (double) (COUNT * 1000L) / (double) (now - start);

            logger.info("{}ms", now - start);
            logger.info("{} requests per second", rqPerSec);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure the PI behavior of stateful and stateless controllers
     * is identical.
     */
    @Test
    void testReconcile() {

        ThreadContext.push("testReconcile");

        try {

            var P = 1d;
            var I = 0.01;
            var Ispan = 1000L;
            var divider = 10;
            var start = System.currentTimeMillis();
            var timestamp = start;
            var delta = 0.1;

            var data = new ArrayList<DataSample<Double>>();

            // Make sure the test timespan doesn't go beyond Ispan,
            // stateful behavior is different then
            for (var count = 0; count < divider - 1; count++) {

                data.add(new DataSample<>(timestamp, "source", "signature", delta, null));
                timestamp += Ispan / divider;
            }

            var pcStateless = new SimplePidController(0, P, I, 0, 0);
            var pcStateful = new PID_Controller(0.0, P, I, Ispan, 0.0, 1, 0);

            for (var pv : data) {

                var signalStateless = pcStateless.compute(pv);
                var signalStateful = pcStateful.compute(pv);

                var offset = signalStateless.timestamp - start;
                logger.debug("signal: {}/{}@{}", signalStateless.sample, signalStateful.sample, offset);

                assertThat(offset).isLessThanOrEqualTo(Ispan);

                assertThat(signalStateful.timestamp).isEqualTo(signalStateless.timestamp);
                assertThat(signalStateful.sample).isEqualTo(signalStateless.sample);

                assertThat(signalStateful.sample.doubleValue()).isEqualTo(I * offset * delta + delta, within(0.00001));
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Test the integral component.
     */
    @Test
    void testI() {

        ThreadContext.push("testI");

        try {

            var P = 1d;
            var I = 0.01;
            var Ispan = 1000L;
            var divider = 10;
            var start = System.currentTimeMillis();
            var timestamp = start;
            var delta = 0.1;
            var limit = I * Ispan * delta + delta;

            var data = new ArrayList<DataSample<Double>>();

            for (var count = 0; count < divider * 2; count++) {
                data.add(new DataSample<>(timestamp, "source", "signature", delta, null));
                timestamp += Ispan / divider;
            }

            ProcessController pc = new PID_Controller(0.0, P, I, Ispan, 0.0, 1, 0);

            for (var datum : data) {
                logger.debug("sample: {}", datum);
            }

            for (var pv : data) {

                var signal = pc.compute(pv);

                var offset = signal.timestamp - start;
                logger.debug("signal: {}@{}", signal, offset);

                if (offset < Ispan) {
                    assertThat(signal.sample).isEqualTo(I * offset * delta + delta, within(0.00001));
                } else {
                    assertThat(signal.sample).isEqualTo(limit);
                }
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Estimate how bad the performance of the integral set is, in real life.
     */
    @Test
    void testIrateSimple() {

        var P = 1d;
        var I = 0.0001;
        var Ispan = 15 * 60 * 1000L;
        var deltaT = 300;
        var start = System.currentTimeMillis();
        var COUNT = (Ispan / deltaT) * 2L;

        var pc = new SimplePidController(0.0, P, I, 0, 0);
        testIrate(pc, start, deltaT, COUNT);
    }

    /**
     * Estimate how bad the performance of the integral set is, in real life.
     */
    @Test
    void testIrateStateful() {

        var P = 1d;
        var I = 0.0001;
        var Ispan = 15 * 60 * 1000;
        var deltaT = 300;
        var start = System.currentTimeMillis();
        var COUNT = (Ispan / deltaT) * 2L;

        var pc = new PID_Controller(0.0, P, I, Ispan, 0.0, 1, 0);
        testIrate(pc, start, deltaT, COUNT);
    }

    /**
     * Estimate how bad the performance of the integral set is, in real life.
     */
    void testIrate(ProcessController pc, long start, int deltaT, long COUNT) {

        ThreadContext.push("testIrate/" + pc.getClass().getName());

        try {

            var timestamp = start;

            logger.info("Count: {}", COUNT);

            for (var count = 0; count < COUNT; count++) {

                var pv = new DataSample<>(
                        timestamp,
                        "source",
                        "signature",
                        rg.nextDouble(),
                        null);

                pc.compute(pv);
                timestamp += rg.nextInt(deltaT) + 1;
            }

            var now = System.currentTimeMillis();
            var rqPerSec = (double) (COUNT * 1000L) / (double) (now - start);

            logger.info((now - start) + "ms");
            logger.info(rqPerSec + " requests per second");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Test the derivative component.
     */
    @Test
    void testD() {

        ThreadContext.push("testD");

        try {

            var P = 1d;
            var D = 1000d;
            var Dspan = 1000L;
            var divider = 10;
            var start = System.currentTimeMillis();
            var timestamp = start;

            var data = new ArrayList<DataSample<Double>>();

            for (var count = 0; count < divider * 2; count++) {
                data.add(new DataSample<>(
                        timestamp,
                        "source",
                        "signature",
                        timestamp == start ? 0.0 : P,
                        null));
                timestamp += Dspan / divider;
            }

            var pc = new PID_Controller(0.0, P, 0, 1, D, Dspan, 0);

            for (var datum : data) {
                logger.debug("sample: {}", datum);
            }

            for (DataSample<Double> pv : data) {

                var signal = pc.compute(pv);
                var offset = signal.timestamp - start;
                logger.debug("signal: {}@{}", signal, offset);
            }

            // VT: NOTE: Very vague condition, need improvement
            assertThat(data.get(data.size() - 1).sample).isEqualTo(P);

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    void testSimplePidControllerID() {
        testPidControllerID(new SimplePidController(20, 1, 0, 0, 0), "simple");
    }

    @Test
    void testTimedPidControllerID() {
        testPidControllerID(new PID_Controller(20, 1.0, 0.0, 1000, 0.0, 1000, 0), "timed");
    }

    private void testPidControllerID(AbstractPidController controller, String ndc) {

        ThreadContext.push("testSimplePidControllerID/" + ndc);

        try {

            for (var timestamp = 0; timestamp < 100; timestamp++) {

                var pv = new DataSample<>(timestamp, "source", "sig", 21.0 + rg.nextInt(10), null);
                controller.compute(pv);

                var signal = (PidControllerStatus) controller.getStatus();
                logger.info(signal);

                assertThat(signal.i).isEqualTo(0.0, within(0.000000000000001));
                assertThat(signal.d).isEqualTo(0.0, within(0.000000000000001));
            }

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    void testSaturationSimple() {
        testSaturationNull(new SimplePidController(20, 1, 1, 0, 3), "simple");
    }

    @Test
    void testSaturationNullTimed() {
        testSaturationNull(new PID_Controller(20, 1.0, 1, 1000, 0.0, 1000, 3), "timed");
    }

    void testSaturationNull(AbstractPidController controller, String ndc) {

        ThreadContext.push("testSimplePidControllerID/" + ndc);

        try {

            for (var timestamp = 0; timestamp < 100; timestamp++) {

                var pv = new DataSample<>(timestamp, "source", "sig", 21.0, null);
                controller.compute(pv);

                var signal = (PidControllerStatus) controller.getStatus();
                logger.info(signal);

                assertThat(signal.d).isEqualTo(0.0, within(0.000000000000001));
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * @see <a href="https://github.com/home-climate-control/dz/issues/155">issue #155</a>
     */
    @Test
    void sensorBlackoutSimplePidController() {

        // Actual arguments from the real configuration
        var setpoint = 24d;
        var P = 0.7;
        var I = 0.000002;
        var D = 0d;
        var saturationLimit = 3;

        sensorBlackout(new SimplePidController(setpoint, P, I, D, saturationLimit), "simple", saturationLimit);
    }

    /**
     * @see <a href="https://github.com/home-climate-control/dz/issues/155">issue #155</a>
     */
    @Test
    void sensorBlackoutPID_Controller() {

        // Actual arguments from the real configuration
        var setpoint = 24d;
        var P = 0.7;
        var I = 0.000002;
        var D = 0d;
        var saturationLimit = 3;
        var span = Duration.of(3, ChronoUnit.HOURS).toMillis();

        sensorBlackout(new PID_Controller(setpoint, P, I, span, D, span, saturationLimit), "full", saturationLimit);
    }

    void sensorBlackout(AbstractPidController pidController, String marker, int saturationLimit) {

        var pidLogger = new PidLogger(marker);
        pidController.addConsumer(pidLogger);

        var source = "source";
        var signature = "signature";
        var start = Clock.systemUTC().instant();
        var minute = new AtomicInteger(0);
        var increment = 60;
        var gap = 120;
        var sampleStream = List.of(
                new DataSample<>(start.toEpochMilli(), source, signature, 25d, null),
                new DataSample<>(start.plus(minute.addAndGet(increment), ChronoUnit.SECONDS).toEpochMilli(), source, signature, 25d, null),
                new DataSample<>(start.plus(minute.addAndGet(increment), ChronoUnit.SECONDS).toEpochMilli(), source, signature, 25d, null),
                new DataSample<>(start.plus(minute.addAndGet(increment), ChronoUnit.SECONDS).toEpochMilli(), source, signature, 25d, null),
                new DataSample<>(start.plus(minute.addAndGet(increment), ChronoUnit.SECONDS).toEpochMilli(), source, signature, 25d, null),
                // BLACKOUT
                new DataSample<>(start.plus(minute.addAndGet(gap), ChronoUnit.MINUTES).toEpochMilli(), source, signature, 25d, null)
        );

        for (var sample : sampleStream) {
            pidController.consume(sample);
        }

        assertThat(pidLogger.samples).hasSize(6);
        assertThat(((PidControllerStatus) pidLogger.samples.get(5).sample).i).isLessThanOrEqualTo(saturationLimit);
    }

    private class PidLogger implements DataSink<ProcessControllerStatus> {

        private final String marker;
        public final List<DataSample<ProcessControllerStatus>> samples = new ArrayList<>();

        public PidLogger(String marker) {
            this.marker = marker;
        }

        @Override
        public void consume(DataSample<ProcessControllerStatus> signal) {
            logger.info("{} sample: {}", marker, signal);
            samples.add(signal);
        }
    }

    @Test
    void testJmxSimple() {

        var mBeanServer = ManagementFactory.getPlatformMBeanServer();
        var beanCount = mBeanServer.getMBeanCount();
        new JmxWrapper().register(new SimplePidController(0, 1, 0, 0, 0));
        assertThat(mBeanServer.getMBeanCount()).isEqualTo(beanCount + 1);
    }

    @Test
    void testJmxFull() {

        var mBeanServer = ManagementFactory.getPlatformMBeanServer();
        var beanCount = mBeanServer.getMBeanCount();
        new JmxWrapper().register(new PID_Controller(0.0, 1, 0.0, 1, 0.0, 1, 0));
        assertThat(mBeanServer.getMBeanCount()).isEqualTo(beanCount + 1);
    }
}
