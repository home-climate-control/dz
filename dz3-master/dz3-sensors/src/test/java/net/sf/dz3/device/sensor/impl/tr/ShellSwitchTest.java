package net.sf.dz3.device.sensor.impl.tr;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

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

    private final Logger logger = LogManager.getLogger(getClass());

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

    public void testGoodSetOpen() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGoodSetOpen");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 7",
                                             "echo 0",
                                             "",
                                             1000);
            logger.info("Set switch open");
            ss.setState(false);
            assertTrue(ss.getCommandOutputValue() == 7);

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {

            logger.error("Unexpected exception",  t);
            fail("test failed, see the log for the exception trace");

        } finally {
            ThreadContext.pop();
        }
    }

    public void testGoodSetClosed() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGoodSetClosed");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 7",
                                             "",
                                             1000);
            logger.info("Set switch closed");
            ss.setState(true);
            assertTrue(ss.getCommandOutputValue() == 7);

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {

            logger.error("Unexpected exception",  t);
            fail("test failed, see the log for the exception trace");

        } finally {
            ThreadContext.pop();
        }
    }

    public void testSetExecFailure() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testSetExecFailure");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "there.is.no.such.command",
                                             "echo 0",
                                             "",
                                             1000);
            logger.info("ShellSwitch set exec failure");
            ss.setState(false);
            fail("ShellSwitch testSetExecFailure did not throw exception");

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {
            assertTrue(true);
        } finally {
            ThreadContext.pop();
        }
    }

    public void testSetNoOutput() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testSetNoOutput");
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

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {

            logger.error("Unexpected exception",  t);
            fail("test failed, see the log for the exception trace");

        } finally {
            ThreadContext.pop();
        }
    }

    public void testSetBadOutput() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testSetBadOutput");
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

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {

            logger.error("Unexpected exception",  t);
            fail("test failed, see the log for the exception trace");

        } finally {
            ThreadContext.pop();
        }
    }

    public void testParseReturn() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testParseReturn");
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

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {

            logger.error("Unexpected exception",  t);
            fail("test failed, see the log for the exception trace");

        } finally {
            ThreadContext.pop();
        }
    }

    public void testGoodGetOpen() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGoodGetOpen");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo 0",
                                             1000);
            logger.info("Get state - expect open");
            boolean testRet = ss.getState();
            assertTrue(testRet == false);

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {

            logger.error("Unexpected exception",  t);
            fail("test failed, see the log for the exception trace");

        } finally {
            ThreadContext.pop();
        }
    }

    public void testGoodGetClosed() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGoodGetClosed");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo 1",
                                             1000);
            logger.info("Get state - expect closed");
            boolean testRet = ss.getState();
            assertTrue(testRet == true);

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {

            logger.error("Unexpected exception",  t);
            fail("test failed, see the log for the exception trace");

        } finally {
            ThreadContext.pop();
        }
    }

    public void testGetLastCommanded() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGetLastCommanded");
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

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {

            logger.error("Unexpected exception",  t);
            fail("test failed, see the log for the exception trace");

        } finally {
            ThreadContext.pop();
        }
    }

    public void testGetExecFailure() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGetExecFailure");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "there.is.no.such.command",
                                             1000);
            logger.info("ShellSwitch get exec failure");
            ss.getState();

            fail("ShellSwitch testGetExecFailure did not throw exception");

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {
            assertTrue(true);
        } finally {
            ThreadContext.pop();
        }
    }

    public void testGetNoOutput() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGetNoOutput");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo",
                                             "echo",
                                             "echo",
                                             1000);
            logger.info("ShellSwitch get no output");
            ss.getState();

            fail("ShellSwitch testSetNoOutput did not throw exception");

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {
            assertTrue(true);
        } finally {
            ThreadContext.pop();
        }
    }

    public void testGetBadOutput() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGetBadOutput");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo QQQ",
                                             1000);
            logger.info("ShellSwitch bad output");
            ss.getState();

            fail("ShellSwitch testGetBadOutput did not throw exception");

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {
            assertTrue(true);
        } finally {
            ThreadContext.pop();
        }
    }

    public void testGetInvalidOutput() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGetInvalidOutput");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo 7",
                                             1000);
            logger.info("ShellSwitch invalid output");
            ss.getState();

            fail("ShellSwitch testGetInvalidOutput did not throw exception");

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {
            assertTrue(true);
        } finally {
            ThreadContext.pop();
        }
    }

    public void testGetInvalidReturn() throws IOException {
        assertTrue(isOsSupported());

        ThreadContext.push("testGetInvalidReturn");
        try {
            ShellSwitch ss = new ShellSwitch("AddrOfTestSwitch",
                                             "echo 0",
                                             "echo 0",
                                             "echo 2",
                                             1000);
            logger.info("ShellSwitch invalid return");
            ss.getState();

            fail("ShellSwitch testSetInvalidOutput did not throw exception");

        } catch (AssertionFailedError ex) {

            throw ex;

        } catch (Throwable t) {
            assertTrue(true);
        } finally {
            ThreadContext.pop();
        }
    }
}
