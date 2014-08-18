package net.sf.dz3.device.sensor;

import junit.framework.TestCase;
import net.sf.dz3.device.sensor.impl.AnalogConverterTMP36;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

public class AnalogConverterTMP36Test extends TestCase {
    
    protected final Logger logger = Logger.getLogger(getClass());

    private final AnalogConverter c = new AnalogConverterTMP36();

    public void testTMP36AnalogReference() {

        NDC.push("TMP36");
        
        try {
            
            printC(1100d);
            printC(2560d);
            printC(5000d);

        } finally {
            NDC.pop();
        }
    }
    
    private void printC(double millivolts) {
        logger.info("Top measurable temperature at " + millivolts + "mV analog reference: " + c.convert(millivolts) + "°C");
    }
}
