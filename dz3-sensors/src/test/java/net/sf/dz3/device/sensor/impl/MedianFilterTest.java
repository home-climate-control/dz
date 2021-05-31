package net.sf.dz3.device.sensor.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.device.sensor.AnalogFilter;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link MedianFilter}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2012-2018
 */
class MedianFilterTest {

    @Test
    public void test3() {

        double sequence[] = {1, 2, 3, 4, 5};
        double match[] =    {1, 2, 2, 3, 4};

        test(3, sequence, match);
    }

    @Test
    public void test3repeated2() {

        double sequence[] = {1, 2, 3, 4, 4};
        double match[] =    {1, 2, 2, 3, 4};

        test(3, sequence, match);
    }

    @Test
    public void test3repeated3() {

        double sequence[] = {1, 2, 3, 4, 4, 4};
        double match[] =    {1, 2, 2, 3, 4, 4};

        test(3, sequence, match);
    }

    @Test
    public void test5() {

        double sequence[] = {1, 2, 3, 4, 5, 6, 7};
        double match[] =    {1, 2, 3, 4, 3, 4, 5};

        test(5, sequence, match);
    }

    @Test
    public void test5repeated2() {

        double sequence[] = {1, 2, 3, 4, 5, 6, 7, 7};
        double match[] =    {1, 2, 3, 4, 3, 4, 5, 6};

        test(5, sequence, match);
    }

    @Test
    public void test5repeated3() {

        double sequence[] = {1, 2, 3, 4, 5, 6, 7, 7, 7};
        double match[] =    {1, 2, 3, 4, 3, 4, 5, 6, 7};

        test(5, sequence, match);
    }

    private void test(int depth, double[] sequence, double[] match) {

        ThreadContext.push("test(" + depth + ")");

        try {

            AnalogSensor source = new NullSensor("source", 1000);
            AnalogFilter mf = new MedianFilter("filter", source, depth);
            Consumer c = new Consumer();

            mf.addConsumer(c);

            for (int offset = 0; offset < sequence.length; offset++) {

                mf.consume(new DataSample<Double>("source", "signature", sequence[offset], null));

                assertThat(c.sample.sample).as("Mismatch at offset " + offset).isEqualTo(match[offset]);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    private static class Consumer implements DataSink<Double> {

        public DataSample<Double> sample;

        @Override
        public void consume(DataSample<Double> sample) {
            this.sample = sample;
        }
    }
}
