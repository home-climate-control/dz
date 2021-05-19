package net.sf.dz3.device.sensor.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.sensor.AnalogFilter;
import net.sf.dz3.device.sensor.AnalogSensor;
import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

/**
 * A median filter on a set of sources.
 *
 * Unlike {@link MedianFilter} which will filter subsequent samples, this filter
 * will yield the median of all available last readings from all the sensors in
 * the set.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2012-2020
 */
public class MedianSetFilter implements AnalogFilter {

    private final Logger logger = LogManager.getLogger(getClass());
    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<>();

    /**
     * Filter address.
     */
    public final String address;

    /**
     * Max sample age to start discarding as stale, in milliseconds.
     *
     * Value of zero means never expire.
     */
    private long maxStaleMillis;

    /**
     * Known sample mapping.
     *
     * The key is the sensor address, the value is the data sample.
     */
    private final Map<String, DataSample<Double>> lastKnown = new LinkedHashMap<>();

    /**
     * Last computed sample.
     */
    private DataSample<Double> currentSignal;

    /**
     * Create a filter with a given address, a set of data sources, and default expiration timeout of a minute.
     *
     * @param address Filter address.
     * @param source Data source set.
     */
    public MedianSetFilter(String address, Set<AnalogSensor> source) {
        this(address, source, 60 * 1000);
    }

    /**
     * Create a filter with a given address, a set of data sources, and expiration timeout.
     *
     * @param address Filter address.
     * @param source Data source set.
     * @param maxStaleMillis Expiration timeout.
     */
    public MedianSetFilter(String address, Set<AnalogSensor> source, long maxStaleMillis) {

        if (address == null || "".equals(address)) {
            throw new IllegalArgumentException("address can't be null or empty");
        }

        if (source == null) {
            throw new IllegalArgumentException("source can't be null");
        }

        if (source.isEmpty()) {
            throw new IllegalArgumentException("empty source, doesn't make sense");
        }

        this.address = address;

        setMaxStaleMillis(maxStaleMillis);

        for (AnalogSensor s : source) {

            s.addConsumer(this);
            lastKnown.put(s.getAddress(),  null);
        }
    }

    @Override
    @JmxAttribute(description = "Current signal")
    public DataSample<Double> getSignal() {

        // VT: NOTE: This signal may be stale. It is the responsibility of the caller
        // to deal with it.

        return currentSignal;
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {
        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {
        dataBroadcaster.removeConsumer(consumer);
    }

    @Override
    @JmxAttribute(description = "Sensor address")
    public final String getAddress() {
        return address;
    }

    @JmxAttribute(description = "Max sample age to take into account, in milliseconds")
    public long getMaxStaleMillis() {
        return maxStaleMillis;
    }

    public void setMaxStaleMillis(long maxStaleMillis) {

        if (maxStaleMillis < 0) {
            throw new IllegalArgumentException("maxStaleMillis can't be negative (" + maxStaleMillis + " given)");
        }

        this.maxStaleMillis = maxStaleMillis;
    }

    /**
     * Consume the sample, expire others if necessary, emit the calculation result.
     *
     * @param sample Sample to consume.
     */
    @Override
    public synchronized void consume(DataSample<Double> sample) {

        ThreadContext.push("consume(" + sample + ")");

        try {

            if (sample == null) {
                throw new IllegalArgumentException("sample can't be null");
            }

            expire(sample.timestamp);

            lastKnown.put(sample.sourceName, sample);

            dataBroadcaster.broadcast(filter(lastKnown.values()));

        } finally {

            ThreadContext.pop();
        }
    }

    private void expire(long timestamp) {

        // To avoid the need to consult real time, we'll just expire the readings that are maxStaleMillis older than
        // the timestamp of the sample we're consuming

        long retire = timestamp - maxStaleMillis;

        logger.debug("retire before {}", new Date(retire));

        for (Iterator<Entry<String, DataSample<Double>>> i = lastKnown.entrySet().iterator(); i.hasNext(); ) {

            Entry<String, DataSample<Double>> e = i.next();

            if (e.getValue() != null && e.getValue().timestamp < retire) {
                logger.debug("expiring {}", e.getValue());
                i.remove();
            }
        }
    }

    /**
     * Filter the samples.
     *
     * @param source Known data samples.
     *
     * @return The median value with the latest timestamp.
     */
    DataSample<Double> filter(Collection<DataSample<Double>> source) {

        SortedSet<Long> timestamps = new TreeSet<>();
        List<Double> samples = new LinkedList<>();

        for (DataSample<Double> s : source) {

            if (s == null) {
                continue;
            }

            timestamps.add(s.timestamp);

            if (s.sample != null) {
                samples.add(s.sample);
            }
        }

        Collections.sort(samples);

        Double sample;
        Throwable error;

        if (!samples.isEmpty()) {

            error = null;
            sample = filter(samples.toArray(new Double[0]));

        } else {

            // All the latest values are errors

            ThreadContext.push("errors");

            for (DataSample<Double> s : source) {

                logger.error("Error sample", s.error);
            }

            ThreadContext.pop();

            sample = null;
            error = new IllegalArgumentException("All samples are errors, see the logs for details");
        }

        currentSignal = new DataSample<>(timestamps.last(), address, address, sample, error);

        return currentSignal;
    }

    Double filter(Double[] array) {

        int size = array.length;

        if (size % 2 == 1) {

            return array[(size - 1) / 2];

        } else {

            double low = array[(size - 1) / 2];
            double high = array[(size - 1) / 2 +1];

            return (high + low) / 2;
        }
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                Integer.toHexString(hashCode()),
                "Return the median signal from " + lastKnown.keySet());
      }
}
