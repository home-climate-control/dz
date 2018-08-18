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
    public void test41_single_nondefault() throws IOException {

        ThreadContext.push("test41_single_nondefault");

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
    public void test41_single_default() throws IOException {

        ThreadContext.push("test41_single_default");

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
    public void test41_multi_nondefault() throws IOException {

        ThreadContext.push("test41_multi_nondefault");

        try {

            testMultiplexer("damper_multiplexer_0", 0.0);

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
    public void test41_multi_default() throws IOException {

        ThreadContext.push("test41_multi_default");

        try {

            testMultiplexer("damper_multiplexer_1", 1.0);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure a damper multiplexer puts its subs into non-default position when it is parked.
     *
     * See https://github.com/home-climate-control/dz/issues/41
     */
    @Test
    public void test41_sub_nondefault() throws IOException {

        ThreadContext.push("test41_sub_nondefault");

        try {

            testMultiplexer("damper_multiplexer_0",
                    "null_switch_0", "switch_damper_0",
                    "null_switch_1", "switch_damper_1",
                    "false:0.0 false:0.0");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure a damper multiplexer puts its subs into default position when it is parked.
     *
     * See https://github.com/home-climate-control/dz/issues/41
     */
    @Test
    public void test41_sub_default() throws IOException {

        ThreadContext.push("test41_sub_default");

        try {

            // VT: NOTE: This test case represents the essence of https://github.com/home-climate-control/dz/issues/41 -
            // even though switch_damper_0 is explicitly requested to be parked at 0.0,
            // it is still parked at 1.0 because this is how the damper multiplexer is coded.

            testMultiplexer("damper_multiplexer_1",
                    "null_switch_0", "switch_damper_0",
                    "null_switch_1", "switch_damper_1",
                    "true:1.0 true:1.0");

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

    /**
     * Make sure the damper multiplexer uts subs at the right positions after it's parked.
     *
     * @param damperId Damper to park.
     * @param subSwitchId0 Sub switch ID 0.
     * @param subDamperId0 Sub damper ID 0.
     * @param subDamperId1 Sub damper ID 1.
     * @param subSwitchId1 Sub switch ID 1.
     * @param expectedState Expected sub switch/damper position.
     */
    private void testMultiplexer(String damperId,
            String subSwitchId0, String subDamperId0,
            String subSwitchId1, String subDamperId1,
            String expectedState) throws IOException {

        AbstractApplicationContext springContext = new ClassPathXmlApplicationContext("dampers.conf.xml");

        Damper damper = (Damper) springContext.getBean(damperId, Damper.class);

        damper.park();

        Damper sub0 = (Damper) springContext.getBean(subDamperId0, Damper.class);
        Damper sub1 = (Damper) springContext.getBean(subDamperId1, Damper.class);

        Switch switch0 = (Switch) springContext.getBean(subSwitchId0, Switch.class);
        Switch switch1 = (Switch) springContext.getBean(subSwitchId1, Switch.class);

        String state0 = switch0.getState() + ":" + sub0.getPosition();
        String state1 = switch1.getState() + ":" + sub1.getPosition();

        String state = state0 + " " + state1;

        assertEquals("wrong parked position", expectedState, state);
    }
}
