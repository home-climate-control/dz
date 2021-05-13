package net.sf.dz3.device.sensor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import junit.framework.TestCase;
import net.sf.dz3.device.sensor.impl.AnalogConverterTMP36;

public class AnalogConverterTMP36Test extends TestCase {
    
    protected final Logger logger = LogManager.getLogger(getClass());

    private final AnalogConverter c = new AnalogConverterTMP36();

    public void testTMP36AnalogReference() {

        ThreadContext.push("TMP36");
        
        try {
            
            printC(1100d);
            printC(2560d);
            printC(5000d);

        } finally {
            ThreadContext.pop();
        }
    }
    
    private void printC(double millivolts) {
        logger.info("Top measurable temperature at " + millivolts + "mV analog reference: " + c.convert(millivolts) + "°C");
    }
}
