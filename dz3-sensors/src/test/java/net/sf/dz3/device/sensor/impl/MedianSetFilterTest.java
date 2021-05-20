package net.sf.dz3.device.sensor.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class MedianSetFilterTest {

    private final static double DELTA = 0.000001;

    @Test
    public void oddArray1() {

        AnalogSensor s = mock(AnalogSensor.class);
        Set<AnalogSensor> source = new HashSet<>();

        source.add(s);
        MedianSetFilter msf = new MedianSetFilter("address", source);

        Double[] samples = { 1d };
        double result = msf.filter(samples);

        assertThat(result).isEqualTo(1d);
    }

    @Test
    public void oddArray3() {

        AnalogSensor s = mock(AnalogSensor.class);
        Set<AnalogSensor> source = new HashSet<>();

        source.add(s);
        MedianSetFilter msf = new MedianSetFilter("address", source);

        Double[] samples = { 1d, 15d, 17d };
        double result = msf.filter(samples);

        assertThat(result).isEqualTo(15d);
    }

    @Test
    public void evenArray2() {

        AnalogSensor s = mock(AnalogSensor.class);
        Set<AnalogSensor> source = new HashSet<>();

        source.add(s);
        MedianSetFilter msf = new MedianSetFilter("address", source);

        Double[] samples = { 1d, 3d };
        double result = msf.filter(samples);

        assertThat(result).isEqualTo(2d);
    }

    @Test
    public void evenArray4() {

        AnalogSensor s = mock(AnalogSensor.class);
        Set<AnalogSensor> source = new HashSet<>();

        source.add(s);
        MedianSetFilter msf = new MedianSetFilter("address", source);

        Double[] samples = { 1d, 3d, 4d, 5d };
        double result = msf.filter(samples);

        assertThat(result).isEqualTo(3.5d);
    }

    @Test
    public void consume() {

        ThreadContext.push("consume");

        try {

            String a1 = "a1";
            String a2 = "a2";
            String a3 = "a3";

            // VT: NOTE: Mocks would do great here, except they don't support mocking final methods

            AnalogSensor s1 = new NullSensor(a1, 1000);
            AnalogSensor s2 = new NullSensor(a2, 1000);
            AnalogSensor s3 = new NullSensor(a3, 1000);

            Set<AnalogSensor> source = new HashSet<>();

            source.add(s1);
            source.add(s2);
            source.add(s3);

            MedianSetFilter msf = new MedianSetFilter("address", source);

            // Happy path

            // 2, null, null
            msf.consume(new DataSample<Double>(1, a1, a1, 2d, null));
            assertThat(msf.getSignal().timestamp).isEqualTo(1);
            assertThat(msf.getSignal().sample).isEqualTo(2d);

            // 2, 1, null
            msf.consume(new DataSample<Double>(2, a2, a2, 1d, null));
            assertThat(msf.getSignal().timestamp).isEqualTo(2);
            assertThat(msf.getSignal().sample).isEqualTo(1.5d);

            // 2, 1, 5
            msf.consume(new DataSample<Double>(3, a3, a3, 5d, null));
            assertThat(msf.getSignal().timestamp).isEqualTo(3);
            assertThat(msf.getSignal().sample).isEqualTo(2d);

            // 2, 1, 4
            msf.consume(new DataSample<Double>(4, a3, a3, 4d, null));
            assertThat(msf.getSignal().timestamp).isEqualTo(4);
            assertThat(msf.getSignal().sample).isEqualTo(2d);

            // 2, 1, 1.5
            msf.consume(new DataSample<Double>(5, a3, a3, 1.5d, null));
            assertThat(msf.getSignal().timestamp).isEqualTo(5);
            assertThat(msf.getSignal().sample).isEqualTo(1.5d);

            // 1, 1, 1.5
            msf.consume(new DataSample<Double>(6, a1, a1, 1d, null));
            assertThat(msf.getSignal().timestamp).isEqualTo(6);
            assertThat(msf.getSignal().sample).isEqualTo(1d);

            // Unhappy path

            // null, 1, 1.5
            msf.consume(new DataSample<Double>(7, a1, a1, null, new Error(a1)));
            assertThat(msf.getSignal()).isNotNull();
            assertThat(msf.getSignal().sample).isNotNull();
            assertThat(msf.getSignal().sample).isEqualTo(1.25d);

            // null, null, 1.5
            msf.consume(new DataSample<Double>(8, a2, a2, null, new Error(a1)));
            assertThat(msf.getSignal()).isNotNull();
            assertThat(msf.getSignal().sample).isNotNull();
            assertThat(msf.getSignal().sample).isEqualTo(1.5d);

            // null, null, null
            msf.consume(new DataSample<Double>(9, a3, a3, null, new Error(a1)));
            assertThat(msf.getSignal()).isNotNull();
            assertThat(msf.getSignal().sample).isNull();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> msf.consume(null))
                    .withMessage("sample can't be null");

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void expire() {

        ThreadContext.push("expire");

        try {

            String a1 = "a1";
            String a2 = "a2";
            String a3 = "a3";

            // VT: NOTE: Mocks would do great here, except they don't support mocking final methods

            AnalogSensor s1 = new NullSensor(a1, 1000);
            AnalogSensor s2 = new NullSensor(a2, 1000);
            AnalogSensor s3 = new NullSensor(a3, 1000);

            Set<AnalogSensor> source = new HashSet<>();

            source.add(s1);
            source.add(s2);
            source.add(s3);

            MedianSetFilter msf = new MedianSetFilter("address", source, 1000);

            // 0, 2, null, null
            msf.consume(new DataSample<Double>(0, a1, a1, 2d, null));
            assertThat(msf.getSignal().sample).isEqualTo(2d);

            // 300, 1, null
            msf.consume(new DataSample<Double>(300, a2, a2, 1d, null));
            assertThat(msf.getSignal().sample).isEqualTo(1.5d);

            // 600, 1, 5
            msf.consume(new DataSample<Double>(600, a3, a3, 5d, null));
            assertThat(msf.getSignal().sample).isEqualTo(2d);

            // 900, 2, 1, 4
            msf.consume(new DataSample<Double>(4, a3, a3, 4d, null));
            assertThat(msf.getSignal().sample).isEqualTo(2d);

            // This one will expire a1 and a2
            // 1500, null, null, 1.25
            msf.consume(new DataSample<Double>(1500, a3, a3, 1.28d, null));
            assertThat(msf.getSignal().sample).isEqualTo(1.28d);

        } finally {
            ThreadContext.pop();
        }
    }
}
