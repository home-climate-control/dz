package net.sf.dz3.device.sensor.impl.onewire;

import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.instrumentation.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Test for {@link OwapiDeviceFactory#getSwitch(String)} and underlying {@link Switch} implementation.
 *  
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2018
 */
class SwitchTest {

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

    @Test
    public void get() throws InterruptedException {
        
        if (!isOsSupported()) {
            return;
        }

        // VT: NOTE: This is for my box, = will most probably fail on yours - change it,
        // or remove the test altogether
        
        OwapiDeviceFactory df = new OwapiDeviceFactory("/dev/ttyUSB0", "regular");

        try {

            assertThat(df.start().waitFor()).isTrue();

        } catch (InterruptedException ex) {

            logger.error("Interrupted during startup", ex);
            fail("Can't initialize DeviceFactory");
        }

        df.getSwitch("5F00000020AB3012:0");

        logger.info("stopping");

        df.stop().waitFor();

        logger.info("done");
    }

    @Test
    public void getGood() throws IOException, InterruptedException {
        
        ThreadContext.push("testGetGood");
        
        try {
        
            if (!isOsSupported()) {
                return;
            }

            // VT: NOTE: This is for my box, = will most probably fail on yours - change it,
            // or remove the test altogether
            
            OwapiDeviceFactory df = new OwapiDeviceFactory("/dev/ttyUSB0", "regular");

            try {

                assertThat(df.start().waitFor()).isTrue();

            } catch (InterruptedException ex) {

                logger.error("Interrupted during startup", ex);
                fail("Can't initialize DeviceFactory");
            }

            df.getLock().writeLock().lock();
            ThreadContext.push("locked");
            try {
            
                Switch s00 = df.getSwitch("5F00000020AB3012:0");
                Switch s01 = df.getSwitch("5F00000020AB3012:1");
                Switch s10 = df.getSwitch("CF00000020AA1512:0");
                Switch s11 = df.getSwitch("CF00000020AA1512:1");

                logger.debug("sleeping");
                Thread.sleep(5000);
                logger.debug("woke up");

                logger.info(s00.getAddress() + ": " + s00.getState());
                logger.info(s01.getAddress() + ": " + s00.getState());
                logger.info(s10.getAddress() + ": " + s00.getState());
                logger.info(s11.getAddress() + ": " + s00.getState());

            } finally {
                df.getLock().writeLock().unlock();
                ThreadContext.pop();
            }
        
            logger.info("stopping");

            df.stop().waitFor();

            logger.info("done");

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void setGood() throws IOException, InterruptedException {
        
        ThreadContext.push("testSetGood");
        
        try {
        
            if (!isOsSupported()) {
                return;
            }

            // VT: NOTE: This is for my box, = will most probably fail on yours - change it,
            // or remove the test altogether
            
            OwapiDeviceFactory df = new OwapiDeviceFactory("/dev/ttyUSB0", "regular");

            try {

                assertThat(df.start().waitFor()).isTrue();

            } catch (InterruptedException ex) {

                logger.error("Interrupted during startup", ex);
                fail("Can't initialize DeviceFactory");
            }

            Marker m = new Marker("lock");

            df.getLock().writeLock().lock();
            
            m.checkpoint("got lock");
            ThreadContext.push("locked");
            try {
            
                Switch s00 = df.getSwitch("5F00000020AB3012:0");
                Switch s01 = df.getSwitch("5F00000020AB3012:1");
                Switch s10 = df.getSwitch("CF00000020AA1512:0");
                Switch s11 = df.getSwitch("CF00000020AA1512:1");

                logger.debug("sleeping");
                Thread.sleep(5000);
                logger.debug("woke up");

                s00.setState(true);
                s01.setState(true);
                s10.setState(true);
                s11.setState(true);

                logger.info(s00.getAddress() + ": " + s00.getState());
                logger.info(s01.getAddress() + ": " + s00.getState());
                logger.info(s10.getAddress() + ": " + s00.getState());
                logger.info(s11.getAddress() + ": " + s00.getState());

                logger.debug("sleeping");
                Thread.sleep(3000);
                logger.debug("woke up");

                s00.setState(false);
                s01.setState(false);
                s10.setState(false);
                s11.setState(false);

                logger.info(s00.getAddress() + ": " + s00.getState());
                logger.info(s01.getAddress() + ": " + s00.getState());
                logger.info(s10.getAddress() + ": " + s00.getState());
                logger.info(s11.getAddress() + ": " + s00.getState());

            } finally {

                df.getLock().writeLock().unlock();
                
                m.close();
                ThreadContext.pop();
            }
            
            logger.info("stopping");

            df.stop().waitFor();

            logger.info("done");

        } finally {
            ThreadContext.pop();
        }
    }
}
