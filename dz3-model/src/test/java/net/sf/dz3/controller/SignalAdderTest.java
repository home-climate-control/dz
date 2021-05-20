package net.sf.dz3.controller;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test cases for {@link SignalAdder}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2012
 */
class SignalAdderTest {

    private long timestamp = 0;

    @Test
    public void testSum() {

        // Set up

        Source s1 = new Source("s1");
        Source s2 = new Source("s2");

        SignalAdder adder = new SignalAdder("adder");

        s1.addConsumer(adder);
        s2.addConsumer(adder);

        adder.put("s1", 1.0);
        adder.put("s2", -2.0);

        Sink sink = new Sink();

        adder.addConsumer(sink);

        // Do the magic

        s1.consume(5.0);
        assertThat(sink.lastKnownSignal.isError()).isTrue();
        assertThat(sink.lastKnownSignal.error.getMessage()).isEqualTo("Don't have all signals yet");

        s2.consume(2.5);
        assertThat(sink.lastKnownSignal.sample).isEqualTo(0.0);

        s1.consume("Ouch!");
        assertThat(sink.lastKnownSignal.isError()).isTrue();
        assertThat(sink.lastKnownSignal.error.getMessage()).isEqualTo("Some signals are errors");
    }

    private class Source implements DataSource<Double> {

        private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();
        private final String signature;

        public Source(String signature) {
            this.signature = signature;
        }

        public void consume(double signal) {
            dataBroadcaster.broadcast(new DataSample<Double>(timestamp++, signature, signature, signal, null));
        }

        public void consume(String error) {
            dataBroadcaster.broadcast(new DataSample<Double>(timestamp++, signature, signature, null, new Error(error)));
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

    private static class Sink implements DataSink<Double> {

        public DataSample<Double> lastKnownSignal;

        @Override
        public void consume(DataSample<Double> signal) {
            this.lastKnownSignal = signal;
        }
    }
}
