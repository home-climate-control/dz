package net.sf.dz3.controller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.logger.LogAware;
import net.sf.jukebox.util.MessageDigestFactory;

import org.apache.log4j.NDC;

/**
 * Analog signal adder.
 * 
 * Consumes signals from different sources. Emits aggregated sum.
 * 
 * @param <Source> Defines the type of the signal source.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2009-2012
 */
public class SignalAdder extends LogAware implements DataSink<Double>, DataSource<Double> {

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();

    /**
     * Adder's name.
     * 
     * Necessary evil to allow instrumentation signature.
     */
    private final String sourceName;

    /**
     * Instrumentation signature.
     */
    private final String signature;

    /**
     * Coefficient map.
     * 
     * The key is the source name, the value is the coefficient the input signal for this source will be
     * multiplied by when calculating the {@link #integral}.
     */
    private final Map<String, Double> source2p = new HashMap<String, Double>();

    private final Map<String, DataSample<Double>> source2signal = new HashMap<String, DataSample<Double>>();

    public SignalAdder(String sourceName) {

        this(sourceName, null);
    }

    /**
     * Create an instance and add it as a listener to given sources.
     * 
     * @param sourceName Name to use in instrumentation.
     * @param source2p See {@link #source2p}.
     */
    public SignalAdder(String sourceName, Map<DataSource<Double>, Map<String, Double>> source2p) {

        if (sourceName == null || "".equals(sourceName)) {
            throw new IllegalArgumentException("sourceName can't be null or empty");
        }

        this.sourceName = sourceName;
        this.signature = new MessageDigestFactory().getMD5(sourceName).substring(0, 19);

        if (source2p != null) {

            for (Iterator<DataSource<Double>> i = source2p.keySet().iterator(); i.hasNext(); ) {

                DataSource<Double> source = i.next();
                Map<String, Double> name2p = source2p.get(source);
                Iterator<Entry<String, Double>> i2 = name2p.entrySet().iterator();
                Entry<String, Double> entry = i2.next();

                this.source2p.put(entry.getKey(), entry.getValue());

                source.addConsumer(this);
            }
        }

        logger.debug("Created '" + sourceName + "', sig=" + signature);
    }

    /**
     * Associate the source with the coefficient.
     * 
     * @param source Signal source.
     * @param p Coefficient to apply to this source's signal.
     */
    public void put(String source, double p) {

        if (source == null) {
            throw new IllegalArgumentException("source can't be null");
        }

        source2p.put(source, p);
    }

    @Override
    public void consume(DataSample<Double> signal) {

        NDC.push("consume");

        try {

            String source = signal.sourceName;
            Double p = source2p.get(source);

            check(source, p);
            check(signal);

            source2signal.put(source, signal);

            double integral = 0;

            for (Iterator<Entry<String, Double>> i = source2p.entrySet().iterator(); i.hasNext(); ) {

                // This is safe because of the relation between EventSource and Source
                Entry<String, Double> entry = i.next();
                String key = entry.getKey();
                p = entry.getValue();

                // Note, this is not the container we're iterating on
                DataSample<Double> value = source2signal.get(key);

                if (value == null) {

                    // Don't have all signals yet, can't compare without unacceptably high bias
                    dataBroadcaster.broadcast(new DataSample<Double>(signal.timestamp, sourceName, signature, null, new IllegalStateException("Don't have all signals yet")));
                    return;
                }

                if (value.isError()) {

                    // Can't calculate the result properly, some signals are missing
                    dataBroadcaster.broadcast(new DataSample<Double>(signal.timestamp, sourceName, signature, null, new IllegalStateException("Some signals are errors")));
                    return;
                }

                integral += value.sample * p; 


            }

            dataBroadcaster.broadcast(new DataSample<Double>(signal.timestamp, sourceName, signature, integral, null));

        } finally {
            NDC.pop();
        }
    }

    private void check(String source, Double p) {

        if (p == null) {
            throw new IllegalArgumentException("Don't know source '" + source + "'");
        }
    }

    private void check(DataSample<Double> signal) {

        if (signal == null) {
            throw new IllegalArgumentException("signal can't be null");
        }
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {

        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {

        dataBroadcaster.removeConsumer(consumer);
    }

}
