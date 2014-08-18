package net.sf.dz3.device.sensor.impl;

import org.apache.log4j.NDC;

import junit.framework.TestCase;
import net.sf.dz3.device.sensor.AnalogFilter;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
 * Test cases for {@link MedianFilter}.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2012
 */
public class MedianFilterTest extends TestCase {
    
    public void test3() {
        
        double sequence[] = {1, 2, 3, 4, 5};
        double match[] =    {1, 2, 2, 3, 4};
        
        test(3, sequence, match);
    }
    
    public void test3repeated2() {
        
        double sequence[] = {1, 2, 3, 4, 4};
        double match[] =    {1, 2, 2, 3, 4};
        
        test(3, sequence, match);
    }

    public void test3repeated3() {
        
        double sequence[] = {1, 2, 3, 4, 4, 4};
        double match[] =    {1, 2, 2, 3, 4, 4};
        
        test(3, sequence, match);
    }

    public void test5() {
        
        double sequence[] = {1, 2, 3, 4, 5, 6, 7};
        double match[] =    {1, 2, 3, 4, 3, 4, 5};
        
        test(5, sequence, match);
    }

    public void test5repeated2() {
        
        double sequence[] = {1, 2, 3, 4, 5, 6, 7, 7};
        double match[] =    {1, 2, 3, 4, 3, 4, 5, 6};
        
        test(5, sequence, match);
    }

    public void test5repeated3() {
        
        double sequence[] = {1, 2, 3, 4, 5, 6, 7, 7, 7};
        double match[] =    {1, 2, 3, 4, 3, 4, 5, 6, 7};
        
        test(5, sequence, match);
    }

    private void test(int depth, double[] sequence, double[] match) {
        
        NDC.push("test(" + depth + ")");
        
        try {

            AnalogSensor source = new NullSensor("source", 1000); 
            AnalogFilter mf = new MedianFilter("filter", source, depth);
            Consumer c = new Consumer();

            mf.addConsumer(c);

            for (int offset = 0; offset < sequence.length; offset++) {

                mf.consume(new DataSample<Double>("source", "signature", sequence[offset], null));

                assertEquals("Mismatch at offset " + offset, match[offset], c.sample.sample);
            }

        } finally {
            NDC.pop();
        }
    }
    
    private class Consumer implements DataSink<Double> {

        public DataSample<Double> sample;
        
        @Override
        public void consume(DataSample<Double> sample) {
            this.sample = sample;
        }
    }
}
