package net.sf.dz3.device.actuator.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.sensor.Switch;

public class SwitchDamperTest {

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Make sure a switch damper puts the switch into non-default position when it is parked.
     *
     * See https://github.com/home-climate-control/dz/issues/41
     */
    @Test
    public void test41a() throws IOException {

        ThreadContext.push("test41a");

        try {

            testParkSingleSwitch("null_switch_0", "switch_damper_0", "false:0.0");

        } finally {
            ThreadContext.pop();
        }
    }
    
    /**
     * Make sure a switch damper puts the switch into default position when it is parked.
     *
     * See https://github.com/home-climate-control/dz/issues/41
     */
    @Test
    public void test41b() throws IOException {

        ThreadContext.push("test41b");

        try {

            testParkSingleSwitch("null_switch_1", "switch_damper_1", "true:1.0");

        } finally {
            ThreadContext.pop();
        }
    }
    
    /**
     * Make sure a damper multiplexer puts itself into default position when it is parked.
     *
     * See https://github.com/home-climate-control/dz/issues/41
     */
    @Test
    public void test41c() throws IOException {

        ThreadContext.push("test41c");

        try {

            testMultiplexer("damper_multiplexer_0", 1.0);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure the switch damper and the switch are in expected state after the damper is parked.
     *
     * Damper and switch state are asserted at the same time, to uncover more problems.
     *
     * @param switchId Switch that the damper uses.
     * @param damperId Damper to park.
     * @param expectedState Expected switch/damper state.
     */
    private void testParkSingleSwitch(String switchId, String damperId, String expectedState) throws IOException {
        
        AbstractApplicationContext springContext = new ClassPathXmlApplicationContext("dampers.conf.xml");

        Damper damper = (Damper) springContext.getBean(damperId, Damper.class);
        Switch targetSwitch = (Switch) springContext.getBean(switchId, Switch.class);

        logger.info("Damper: " + damper);
        logger.info("Switch: " + targetSwitch);
        
        damper.park();
        
        String state = targetSwitch.getState() + ":" + damper.getPosition();
        
        logger.info("parked state: " + state);
        
        assertEquals("wrong parked state", expectedState, state);
    }

    /**
     * Make sure the damper multiplexer itself is in at the right position after it's parked.
     *
     * @param damperId Damper to park.
     * @param expectedPosition Expected damper position.
     */
    private void testMultiplexer(String damperId, double expectedPosition) throws IOException {

        AbstractApplicationContext springContext = new ClassPathXmlApplicationContext("dampers.conf.xml");

        Damper damper = (Damper) springContext.getBean(damperId, Damper.class);

        damper.park();

        assertEquals("wrong parked position", expectedPosition, damper.getPosition(), 0.0001);
    }
}
