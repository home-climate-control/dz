package net.sf.dz3.device.sensor.impl.onewire;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import junit.framework.TestCase;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.service.ActiveService;

public class DeviceFactoryTest extends TestCase {
    
    private final Logger logger = LogManager.getLogger(getClass());
    
    /**
     * @return {@code false} if OS is not within a supported list (just Linux for now).
     */
    private boolean isOsSupported() {
        
        Set<String> supported = new TreeSet<String>();
        
        // Uncomment to run your tests - NOT ON THE BOX WHERE DZ IS RUNNING!
        //supported.add("Linux");
        
        String os = System.getProperty("os.name");
        
        if (supported.contains(os)) {
            return true;
        }
        
        logger.error("OS not supported: " + os);
        return false;
    }
    
    public void testDummy() {
        
        // To make JUnit happy when the other test is disabled
    }

    /**
     * Test the {@link OwapiDeviceFactory} initialization.
     * @throws InterruptedException if it is thrown by {@link ActiveService#startup()}.
     */
    public void testFactory() throws InterruptedException {
        
        ThreadContext.push("testFactory");
        
        try {
        
            if (!isOsSupported()) {
                return;
            }

            // VT: NOTE: This is for my box, = will most probably fail on yours - change it,
            // or remove the test altogether

            OwapiDeviceFactory df = new OwapiDeviceFactory("/dev/ttyUSB0", "regular");

            boolean result = df.start().waitFor();

            logger.info("start returned: " + result);

            Thread.sleep(5000);

            logger.info("stopping");

            df.stop().waitFor();

            logger.info("done");
        
        } finally {
            ThreadContext.pop();
        }
    }
   
    public void testGetSensorDelayed() throws InterruptedException {
        
        testGetSensor(5000, 10000);
    }

    public void testGetSensorImmediate() throws InterruptedException {
        
        testGetSensor(0, 10000);
    }

    /**
     * Test {@link OwapiDeviceFactory#getTemperatureSensor(String)} functionality.
     * 
     * This test requires that the device with the given address is physically present on the bus.
     * 
     * @param initialDelay Wait this much after 1-Wire bus startup before trying to get the sensor.
     * @param delay Wait this much after the sensor is acquired.
     * 
     * @throws InterruptedException unlikely.
     */
    private void testGetSensor(long initialDelay, long delay) throws InterruptedException {
        
        ThreadContext.push("testGetSensor(" + initialDelay + ", " + delay + ")");
        
        try {
        
            if (!isOsSupported()) {
                return;
            }

            // VT: NOTE: This is for my box, = will most probably fail on yours - change it,
            // or remove the test altogether

            OwapiDeviceFactory df = new OwapiDeviceFactory("/dev/ttyUSB0", "regular");

            assertTrue("Failed to start, check the logs", df.start().waitFor());

            Thread.sleep(initialDelay);

            AnalogSensor sensor = df.getTemperatureSensor("6500000055FF1A26");

            logger.info("Sensor: " + sensor);

            assertNotNull(sensor);

            List<DataSample<Double>> sink = new LinkedList<DataSample<Double>>();

            sensor.addConsumer(new Sink(sink));

            Thread.sleep(delay);

            logger.info("stopping");

            df.stop().waitFor();

            logger.info("done, " + sink.size() + " samples collected: ");

            for (Iterator<DataSample<Double>> i = sink.iterator(); i.hasNext(); ) {

                logger.debug(i.next());
            }

            assertFalse("Should have collected data samples", sink.isEmpty());

            boolean gotData = false;

            for (Iterator<DataSample<Double>> i = sink.iterator(); i.hasNext(); ) {

                DataSample<Double> sample = i.next();

                if (!sample.isError()) {
                    gotData = true;
                    break;
                }
            }

            assertTrue("No non-error samples were collected", gotData);
        } finally {
            ThreadContext.pop();
        }
    }
    
    private class Sink implements DataSink<Double> {
        
        private final List<DataSample<Double>> sink;
        
        public Sink(List<DataSample<Double>> sink) {
            this.sink = sink;
        }

        @Override
        public void consume(DataSample<Double> signal) {
            
            logger.info("SIGNAL: " + signal);
            sink.add(signal);
        }
        
    }
}
