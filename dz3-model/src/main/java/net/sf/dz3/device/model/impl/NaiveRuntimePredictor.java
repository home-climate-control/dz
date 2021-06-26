package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import net.sf.dz3.device.model.HvacSignal;
import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Runtime predictor producing a simple estimate with a minimum computing power consumption.
 *
 * Instances of this class are thread safe, but stateful.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class NaiveRuntimePredictor implements RuntimePredictor, Comparable<NaiveRuntimePredictor> {

    private final Logger logger = LogManager.getLogger();

    private final String name;
    private final DataBroadcaster<UnitRuntimePredictionSignal> broadcaster = new DataBroadcaster<>();

    /**
     * The moment when the unit started, according to the change of {@link HvacSignal#running}.
     * If the unit is not currently running or never been observed running, {@code null}.
     */
    private Instant start;

    /**
     * Demand at the beginning of the run cycle. Almost.
     *
     * @see #consumeRunning(DataSample)
     */
    private double startDemand;

    /**
     * Create an instance not attached to anything, with a random name.
     */
    public NaiveRuntimePredictor() {
        this.name = Integer.toHexString(hashCode());
    }

    /**
     * Create an instance attached to a data source.
     *
     * @param name Human readable name. Will propagate to UI and metrics collectors.
     * @param source Data source to listen to.
     */
    public NaiveRuntimePredictor(String name, DataSource<HvacSignal> source) {
        this.name = name;
        source.addConsumer(this);
    }

    public String getName() {
        return name;
    }

    /**
     * Consume the signal and broadcast the computed result.
     *
     * @param signal Sample to consume.
     */
    @Override
    public void consume(DataSample<HvacSignal> signal) {

        if (signal == null) {
            throw new IllegalArgumentException("signal can't be null");
        }

        if (signal.isError()) {
            throw new IllegalStateException("can't process errors");
        }

        if (start == null) {
            broadcaster.broadcast(log(consumeIdle(signal)));
        } else {
            broadcaster.broadcast(log(consumeRunning(signal)));
        }
    }

    private DataSample<UnitRuntimePredictionSignal> log(DataSample<UnitRuntimePredictionSignal> signal) {
        LogManager.getLogger().debug(
                "signal: k={}, left={}, arrival={}, plus={}",
                signal.sample.k, signal.sample.left, signal.sample.arrival, signal.sample.plus);
        return signal;
    }

    private DataSample<UnitRuntimePredictionSignal> consumeIdle(DataSample<HvacSignal> signal) {

        if (!signal.sample.running) {

            // Didn't run before, doesn't run now.
            return unknown(signal, 0, false);
        }

        start = Instant.ofEpochMilli(signal.timestamp);
        startDemand = signal.sample.demand;

        logger.info("started runtime, demand={}", startDemand);

        // We can't produce accurate estimate until some time has passed
        return unknown(signal, 0, false);
    }

    private DataSample<UnitRuntimePredictionSignal> consumeRunning(DataSample<HvacSignal> signal) {

        var now = Instant.ofEpochMilli(signal.timestamp);
        var runningFor = Duration.of(signal.sample.uptime, ChronoUnit.MILLIS);

        if (!signal.sample.running) {

            // Easy case, we've arrived.

            start = null;
            startDemand = 0d;

            // ... and we have no idea when the next run is going to end.

            return unknown(signal, 0, false);
        }

        logger.info("uptime={}", signal.sample.uptime);
        logger.info("runningFor={}", runningFor);

        if (runningFor.compareTo(Duration.of(1, ChronoUnit.MINUTES)) < 0) {

            // Can't reliably determine the estimate within the first minute - there is almost inevitably
            // a "hot blow" for cooling mode and "cold blow" for heating mode, this moves the demand the wrong way.
            // However, the starting demand itself can be calculated.

            var oldDemand = startDemand;
            startDemand = Math.max(signal.sample.demand, startDemand);
            logger.info("adjusted startDemand from {} to {}", oldDemand, startDemand);

            return unknown(signal, 0, false);
        }

        // VT: FIXME: Use a simplified version of MedianFilter here, the noise is quite bad

        var k = (startDemand - signal.sample.demand) / runningFor.toMillis();

        if (Double.compare(startDemand, signal.sample.demand) <= 0) {

            // Not good at all. Save from noise, this is a good indication that the unit is not keeping up with demand.

            logger.info("negative delta from {}, likely not keeping up with demand", signal.sample);
            return unknown(signal, k, true);
        }

        var left = Duration.of((long)(signal.sample.demand / k), ChronoUnit.MILLIS);
        var arrival = now.plus(left);

        logger.info("calculated: dD={}, uptime={}, k={}, left={}, arrival={}",
                startDemand - signal.sample.demand, runningFor.toMillis(),
                k, left, arrival);

        final var threeHours = Duration.of(3, ChronoUnit.HOURS);
        boolean plus = left.compareTo(threeHours) > 0;

        if (plus) {
            // That's preposterous. Nobody will care for exact time beyond that, and charts will be screwed up.
            left = threeHours;
            arrival = now.plus(threeHours);
        }

        return new DataSample<>(
                signal.timestamp,
                signal.sourceName,
                signal.signature,
                new UnitRuntimePredictionSignal(signal.sample, k, left, arrival, plus),
                null);
    }

    /**
     * Produce an "unknown" sample.
     *
     * @param signal Incoming signal to use as a template.
     * @param k K value to pass down - it will be useful in analytics and alerting.
     * @param plus {@link UnitRuntimePredictionSignal#plus} value to pass.
     *
     * @return A sample indicating that the prediction cannot be made.
     */
    private DataSample<UnitRuntimePredictionSignal> unknown(DataSample<HvacSignal> signal, double k, boolean plus) {

        return new DataSample<>(
                signal.timestamp,
                signal.sourceName,
                signal.signature,
                new UnitRuntimePredictionSignal(signal.sample, k, null, null, plus),
                null);
    }

    @Override
    public void addConsumer(DataSink<UnitRuntimePredictionSignal> consumer) {
        broadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<UnitRuntimePredictionSignal> consumer) {
        broadcaster.removeConsumer(consumer);
    }

    @Override
    public int compareTo(NaiveRuntimePredictor o) {
        return getName().compareTo((o.getName()));
    }
}
