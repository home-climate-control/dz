package net.sf.dz3.runtime;
import java.lang.reflect.Method;

import junit.framework.TestCase;
import net.sf.dz3.device.sensor.AnalogSensor;

import org.apache.log4j.Logger;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class ReflectionTest extends TestCase {
	
	private final Logger logger = Logger.getLogger(getClass());
	
	private final String PROBLEM_METHOD = "getSensor";
	private final NativeSensorFactory nativeFactory = new NativeSensorFactory();
	
	public void testGetMethod() {
		
		try {
			
			nativeFactory.getClass().getMethod(PROBLEM_METHOD);
			fail("Method should not have been resolved");
		
		} catch (NoSuchMethodException ex) {
			
			// This is the expected result
			
		} catch (Throwable t) {

			fail("Wrong exception class: " + t.getClass().getName());
		}
	}
	
	public void testMethodMatching() {

		Method[] methods = nativeFactory.getClass().getMethods();
		
    	for (int offset = 0; offset < methods.length; offset++) {
    		
    		Method m = methods[offset];
    		
    		if (m.getName().equals(PROBLEM_METHOD)) {

    			// This is the expected result
    			logger.info("Found: " + m);
    			return;
    		}
    	}
    	
    	fail("Failed to find method: " + PROBLEM_METHOD);
	}
	
	public void testSpring() {
		
		AbstractApplicationContext springContext = new ClassPathXmlApplicationContext("dz.conf.xml");
		
		NativeSensorDescriptor descriptor = (NativeSensorDescriptor) springContext.getBean("native_sensor_descriptor", NativeSensorDescriptor.class);

		logger.info("Descriptor: " + descriptor);

		AnalogSensor sensor = (AnalogSensor) springContext.getBean("native_sensor", AnalogSensor.class);
		
		logger.info("Sensor: " + sensor);
	}
}
