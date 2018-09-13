package net.sf.dz3.device.sensor.impl;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.dz3.device.sensor.impl.ShellSwitch;

/**
 * Shell switch tests.
 *
 * These tests support Unix systems.
 *
 * @author Copyright &copy; Thomas Rooney 2015
 * Based on ShellSensorTest by
 * <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2009
 *
 * Note:  Test strings are Unix specific
 */
// TODO: add capability to test timeout
public class ShellSwitchTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());

    protected void setUp(){
      // no action
    }

    protected void tearDown(){
      // no action
    }

    /**
     * @return {@code false} if OS is not within a supported list
     */
    private boolean isOsSupported() {

        Set<String> supported = new TreeSet<String>();

        supported.add("Linux");

        String os = System.getProperty("os.name");

        if (supported.contains(os)) {
            return true;
        }

        logger.error("OS not supported: " + os);
        return false;
    }

    public void testOsSupported() throws IOException {
        NDC.push("testOsIsSupported");
        assertTrue(isOsSupported());
        NDC.pop();
    }

    public void testGoodSetOpen() throws IOException {
        NDC.push("testGoodSetOpen");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 7",
                                             "echo 0",
                                             "",
                                             1000);
            logger.info("Set switch open");
            ss.setState(false);
            assertTrue(ss.getCommandOutputValue() == 7);
        } catch (Exception err) {
            fail("ShellSwitch testGoodSetOpen failed by exception");
        } finally {
            NDC.pop();
        }
    }

    public void testGoodSetClosed() throws IOException {
        NDC.push("testGoodSetClosed");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 7",
                                             "",
                                             1000);
            logger.info("Set switch closed");
            ss.setState(true);
            assertTrue(ss.getCommandOutputValue() == 7);
        } catch (Exception err) {
            fail("ShellSwitch testGoodSetClosed failed by exception");
        } finally {
            NDC.pop();
        }
    }

    public void testSetExecFailure() throws IOException {
        NDC.push("testSetExecFailure");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "there.is.no.such.command",
                                             "echo 0",
                                             "",
                                             1000);
            logger.info("ShellSwitch set exec failure");
            ss.setState(false);
            fail("ShellSwitch testSetExecFailure did not throw exception");
        } catch (Exception err) {
            assertTrue(true);
        } finally {
            NDC.pop();
        }
    }

    public void testSetNoOutput() throws IOException {
        NDC.push("testSetNoOutput");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo",
                                             "echo",
                                             "",
                                             1000);
            logger.info("ShellSwitch set no output");
            ss.setState(false);
            if (ss.getOutputValueRead() == false)
            {
                assert(true);
            }
            else
            {
                fail("ShellSwitch testSetNoOutput did not set m_outputValueRead to false");
            }
        } catch (Exception err) {
            fail("ShellSwitch exception testing command with no output");
        } finally {
            NDC.pop();
        }
    }

    public void testSetBadOutput() throws IOException {
        NDC.push("testSetBadOutput");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo QQQ",
                                             "echo QQQ",
                                             "",
                                             1000);
            logger.info("ShellSwitch bad output");
            ss.setState(false);
            if (ss.getOutputValueRead() == false)
            {
                assert(true);
            }
            else
            {
                fail("ShellSwitch testSetBadOutput did not set m_outputValueRead to false");
            }
        } catch (Exception err) {
            fail("ShellSwitch exception testing command with no output");
        } finally {
            NDC.pop();
        }
    }

    public void testParseReturn() throws IOException {
        NDC.push("testParseReturn");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 2",
                                             "echo 0",
                                             "",
                                             1000);
            ss.setState(false);
            if (ss.getOutputValueRead() == true)
            {
                logger.debug("testParseReturn() output was '" 
                             + Long.toString(ss.getCommandOutputValue()) + "'");

                if (ss.getCommandOutputValue() == 2)
                {
                    assert(true);
		}
                else
                {
                    fail("ShellSwitch testParseReturn output value not equal to expected value '2'");
                }
            }
            else
            {
                fail("ShellSwitch testParseReturn did not set m_outputValueRead to true");
            }
        } catch (Exception err) {
            fail("ShellSwitch exception testing parse command output");
        } finally {
            NDC.pop();
        }
    }

    public void testGoodGetOpen() throws IOException {
        NDC.push("testGoodGetOpen");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo 0",
                                             1000);
            logger.info("Get state - expect open");
            boolean testRet = ss.getState();
            assertTrue(testRet == false);
        } catch (Exception err) {
            fail("ShellSwitch testGoodGetOpen failed by exception");
        } finally {
            NDC.pop();
        }
    }

    public void testGoodGetClosed() throws IOException {
        NDC.push("testGoodGetClosed");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo 1",
                                             1000);
            logger.info("Get state - expect closed");
            boolean testRet = ss.getState();
            assertTrue(testRet == true);
        } catch (Exception err) {
            fail("ShellSwitch testGoodGetClosed failed by exception");
        } finally {
            NDC.pop();
        }
    }

    public void testGetLastCommanded() throws IOException {
        NDC.push("testGetLastCommanded");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "",
                                             1000);
            logger.info("Get state - last command value");
            // open
            ss.setState(false);
            boolean shouldBeFalse = ss.getState();
            assertTrue(shouldBeFalse == false);
            // close
            ss.setState(true);
            boolean shouldBeTrue = ss.getState();
            assertTrue(shouldBeTrue == true);
            // open
            ss.setState(false);
            shouldBeFalse = ss.getState();
            assertTrue(shouldBeFalse == false);
        } catch (Exception err) {
            fail("ShellSwitch testGetLastCommanded failed by exception");
        } finally {
            NDC.pop();
        }
    }

    public void testGetExecFailure() throws IOException {
        NDC.push("testGetExecFailure");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "there.is.no.such.command",
                                             1000);
            logger.info("ShellSwitch get exec failure");
            boolean testRet = ss.getState();
            fail("ShellSwitch testGetExecFailure did not throw exception");
        } catch (Exception err) {
            assertTrue(true);
        } finally {
            NDC.pop();
        }
    }

    public void testGetNoOutput() throws IOException {
        NDC.push("testGetNoOutput");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo",
                                             "echo",
                                             "echo",
                                             1000);
            logger.info("ShellSwitch get no output");
            boolean testRet = ss.getState();
            fail("ShellSwitch testSetNoOutput did not throw exception");
        } catch (Exception err) {
            assertTrue(true);
        } finally {
            NDC.pop();
        }
    }

    public void testGetBadOutput() throws IOException {
        NDC.push("testGetBadOutput");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo QQQ",
                                             1000);
            logger.info("ShellSwitch bad output");
            boolean testRet = ss.getState();
            fail("ShellSwitch testGetBadOutput did not throw exception");
        } catch (Exception err) {
            assertTrue(true);
        } finally {
            NDC.pop();
        }
    }

    public void testGetInvalidOutput() throws IOException {
        NDC.push("testGetInvalidOutput");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo 7",
                                             1000);
            logger.info("ShellSwitch invalid output");
            boolean testRet = ss.getState();
            fail("ShellSwitch testGetInvalidOutput did not throw exception");
        } catch (Exception err) {
            assertTrue(true);
        } finally {
            NDC.pop();
        }
    }

    public void testGetInvalidReturn() throws IOException {
        NDC.push("testGetInvalidReturn");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo 2",
                                             1000);
            logger.info("ShellSwitch invalid return");
            boolean testRet = ss.getState();
            fail("ShellSwitch testSetInvalidOutput did not throw exception");
        } catch (Exception err) {
            assertTrue(true);
        } finally {
            NDC.pop();
        }
    }


}
