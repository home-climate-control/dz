package net.sf.dz3.runtime;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.fail;


class ReflectionTest {

    private final Logger logger = LogManager.getLogger(getClass());
    private final NativeSensorFactory nativeFactory = new NativeSensorFactory();

    private static final String PROBLEM_METHOD = "getSensor";

    @Test
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

    @Test
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

    @Test
    public void testSpring() {

        AbstractApplicationContext springContext = new ClassPathXmlApplicationContext("dz.conf.xml");

        NativeSensorDescriptor descriptor = (NativeSensorDescriptor) springContext.getBean("native_sensor_descriptor", NativeSensorDescriptor.class);

        logger.info("Descriptor: " + descriptor);

        AnalogSensor sensor = (AnalogSensor) springContext.getBean("native_sensor", AnalogSensor.class);

        logger.info("Sensor: " + sensor);
    }
}
