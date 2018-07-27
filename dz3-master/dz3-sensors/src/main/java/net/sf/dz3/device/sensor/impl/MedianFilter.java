package net.sf.dz3.device.sensor.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.sensor.AnalogFilter;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * A median filter.
 * 
 * Careful, first ({@link #depth} - 1) samples will get out unfiltered. 
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2012-2018
 */
public class MedianFilter implements AnalogFilter {

    private final Logger logger = LogManager.getLogger(getClass());
    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();
    private final List<Double> buffer = new LinkedList<Double>();

    public final String address;
    
    /**
     * Filter depth.
     */
    public final int depth;

    public MedianFilter(String address, AnalogSensor source, int depth) {
        
        if (address == null || "".equals(address)) {
            throw new IllegalArgumentException("address can't be null");
        }
        
        if (source == null) {
            throw new IllegalArgumentException("source can't be null, makes no sense");
        }
        
        if (address.equals(source.getAddress())) {
            throw new IllegalArgumentException("address can't be the same as the source address");
        }
        
        if (depth < 3) {
            throw new IllegalArgumentException("depth < 3 makes no sense");
        }
        
        if (depth %2 == 0) {
            throw new IllegalArgumentException("depth has to be an odd number");
        }
        
        this.address = address;
        this.depth = depth;
        
        source.addConsumer(this);
    }

    @Override
    @JmxAttribute(description = "Current signal")
    public DataSample<Double> getSignal() {
        // TODO Auto-generated method stub
        throw new Error("Not Implemented");
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

    @JmxAttribute(description = "Filter depth")
    public final int getDepth() {
        return depth;
    }

    @Override
    public synchronized void consume(DataSample<Double> sample) {
        
        ThreadContext.push("consume(" + sample + ")");
        
        try {

            if (sample == null) {
                throw new IllegalArgumentException("sample can't be null");
            }

            if (sample.sample == null) {

                // Filter behavior must be as transparent as possible.
                // Null sample means trouble, need to pass it on.

                dataBroadcaster.broadcast(mirror(sample));
                return;
            }

            buffer.add(sample.sample);

            if (buffer.size() < depth) {

                logger.debug("buffer too small (" + buffer.size() + " < "  + depth + ")");
                dataBroadcaster.broadcast(mirror(sample));
                return;
            }

            if (buffer.size() > depth) {
                logger.debug("removing first element from " + buffer);
                buffer.remove(0);
            }

            dataBroadcaster.broadcast(filter(sample));

        } finally {
            
            logger.debug("buffer: " + buffer);
            ThreadContext.pop();
        }
    }
    
    private DataSample<Double> mirror(DataSample<Double> source) {
        return new DataSample<Double>(source.timestamp, address, address, source.sample, source.error);
    }
    
    private DataSample<Double> filter(DataSample<Double> source) {
        
        ThreadContext.push("filter");
        
        try {

            // By this time, the buffer contains exactly #depth elements
            List<Double> sorted = new LinkedList<Double>(buffer);
            Collections.sort(sorted);
            
            Double[] array = sorted.toArray(new Double[0]);
            double median = array[(depth - 1) / 2];

            return new DataSample<Double>(source.timestamp, address, address, median, source.error);

        } finally {
            ThreadContext.pop();
        }
    }
    
    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                Integer.toHexString(hashCode()),
                "Filter the signal using median filter algorithm with depth " + depth);
      }
}
