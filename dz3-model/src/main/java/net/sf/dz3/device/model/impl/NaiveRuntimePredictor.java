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
public class NaiveRuntimePredictor implements RuntimePredictor {

    private final Logger logger = LogManager.getLogger();

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
     * Create an instance not attached to anything.
     */
    public NaiveRuntimePredictor() {
    }

    /**
     * Create an instance attached to a data source.
     *
     * @param source Data source to listen to.
     */
    public NaiveRuntimePredictor(DataSource<HvacSignal> source) {
        source.addConsumer(this);
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
            broadcaster.broadcast(consumeIdle(signal));
        } else {
            broadcaster.broadcast(consumeRunning(signal));
        }
    }

    private DataSample<UnitRuntimePredictionSignal> consumeIdle(DataSample<HvacSignal> signal) {

        if (!signal.sample.running) {

            // Didn't run before, doesn't run now.
            return unknown(signal, 0);
        }

        start = Instant.ofEpochMilli(signal.timestamp);
        startDemand = signal.sample.demand;

        // We can't produce accurate estimate until some time has passed
        return unknown(signal, 0);
    }

    private DataSample<UnitRuntimePredictionSignal> consumeRunning(DataSample<HvacSignal> signal) {

        var now = Instant.ofEpochMilli(signal.timestamp);
        var runningFor = Duration.of(signal.sample.uptime, ChronoUnit.MILLIS);

        if (!signal.sample.running) {

            // Easy case, we've arrived.

            start = null;
            startDemand = 0d;

            // ... and we have no idea when the next run is going to end.

            return unknown(signal, 0);
        }

        if (runningFor.compareTo(Duration.of(1, ChronoUnit.MINUTES)) < 0) {

            // Can't reliably determine the estimate within the first minute - there is almost inevitably
            // a "hot blow" for cooling mode and "cold blow" for heating mode, this moves the demand the wrong way.
            // However, the starting demand itself can be calculated.

            startDemand = Math.max(signal.sample.demand, startDemand);

            return unknown(signal, 0);
        }

        // VT: FIXME: Use a simplified version of MedianFilter here, the noise is quite bad

        var k = (startDemand - signal.sample.demand) / runningFor.toMillis();

        if (Double.compare(startDemand, signal.sample.demand) <= 0) {

            // Not good at all. Save from noise, this is a good indication that the unit is not keeping up with demand.

            logger.info("negative delta from {}, likely not keeping up with demand", signal.sample);
            return unknown(signal, k);
        }

        var left = Duration.of((long)(signal.sample.demand / k), ChronoUnit.MILLIS);
        var arrival = now.plus(left);

        return new DataSample<>(
                signal.timestamp,
                signal.sourceName,
                signal.signature,
                new UnitRuntimePredictionSignal(signal.sample, k, left, arrival),
                null);
    }

    /**
     * Produce an "unknown" sample.
     *
     * @param signal Incoming signal to use as a template.
     * @param k K value to pass down - it will be useful in analytics and alerting.
     *
     * @return A sample indicating that the prediction cannot be made.
     */
    private DataSample<UnitRuntimePredictionSignal> unknown(DataSample<HvacSignal> signal, double k) {

        return new DataSample<>(
                signal.timestamp,
                signal.sourceName,
                signal.signature,
                new UnitRuntimePredictionSignal(signal.sample, k, null, null),
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
}