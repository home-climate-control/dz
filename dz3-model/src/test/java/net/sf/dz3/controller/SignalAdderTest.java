package net.sf.dz3.controller;

import junit.framework.TestCase;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;

/**
 * Test cases for {@link SignalAdder}.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2009-2012
 */
public class SignalAdderTest extends TestCase {
    
    private long timestamp = 0;
    
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
        assertTrue(sink.lastKnownSignal.isError());
        assertEquals("Don't have all signals yet", sink.lastKnownSignal.error.getMessage());
        
        s2.consume(2.5);
        assertEquals(0.0, sink.lastKnownSignal.sample);
        
        s1.consume("Ouch!");
        assertTrue(sink.lastKnownSignal.isError());
        assertEquals("Some signals are errors", sink.lastKnownSignal.error.getMessage());
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
