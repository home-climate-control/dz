package net.sf.dz3.device.actuator.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import net.sf.dz3.device.actuator.Damper;

public class DamperMultiplexerTest {

    /**
     * Make sure all dampers are parked at default positions when {@link DamperMultiplexer#park()} is called.
     */
    @Test
    public void testParkAllDefault() throws InterruptedException, ExecutionException, IOException {

        ThreadContext.push("testParkAllDefault");

        try {

            Damper a = new NullDamper("a");
            Damper b = new NullDamper("b");
            Set<Damper> dampers = new HashSet<>();

            dampers.add(a);
            dampers.add(b);

            DamperMultiplexer m = new DamperMultiplexer("m", dampers);

            m.park().get();

            assertEquals("wrong parked position - a", 1.0, a.getPosition(), 0.001);
            assertEquals("wrong parked position - b", 1.0, b.getPosition(), 0.001);
            assertEquals("wrong parked position - m", 1.0, m.getPosition(), 0.001);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure all dampers are parked at the position specified by {@link DamperMultiplexer#getParkPosition()}
     * if there were no defaults specified for each of the subs.
     *
     * This is the "old" behavior (see https://github.com/home-climate-control/dz/issues/51), and
     * it must not change if individual dampers' defaults were not actually set, either via constructor
     * argument, or by calling {@code setParkPosition()}.
     */
    @Test
    public void testParkAllCustomMulti() throws InterruptedException, ExecutionException, IOException {

        ThreadContext.push("testParkAllCustomMulti");

        try {

            Damper a = new NullDamper("a");
            Damper b = new NullDamper("b");
            Set<Damper> dampers = new HashSet<>();

            dampers.add(a);
            dampers.add(b);

            DamperMultiplexer m = new DamperMultiplexer("m", dampers);

            double parkPosition = 0.75;
            m.setParkPosition(parkPosition);

            m.park().get();

            // Note: None of the dampers have their park position specified, hence,
            // they will be parked where multiplexer wants

            assertEquals("wrong parked position - a", parkPosition, a.getPosition(), 0.001);
            assertEquals("wrong parked position - b", parkPosition, b.getPosition(), 0.001);
            assertEquals("wrong parked position - m", parkPosition, m.getPosition(), 0.001);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Make sure all dampers are parked at the position specified by their own defaults (and not
     * the default specified for the {@code DamperMultiplexer}
     *
     * This is the "new" behavior (see https://github.com/home-climate-control/dz/issues/51).
     * Before, there were no facilities to implement it.
     */
    @Test
    public void testParkAllCustom() throws InterruptedException, ExecutionException, IOException {

        ThreadContext.push("testParkAllCustom");

        try {

            Damper a = new NullDamper("a");
            Damper b = new NullDamper("b");
            Damper c = new NullDamper("c");
            Set<Damper> dampers = new HashSet<>();

            dampers.add(a);
            dampers.add(b);
            dampers.add(c);

            DamperMultiplexer m = new DamperMultiplexer("m", dampers);

            double parkPositionA = 0.75;
            double parkPositionB = 0.25;
            double parkPositionM = 0.50;

            a.setParkPosition(parkPositionA);
            b.setParkPosition(parkPositionB);
            m.setParkPosition(parkPositionM);

            // Note: Damper 'c' will have the default park position (none), hence it will be overridden

            m.park().get();

            assertEquals("wrong parked position - a", parkPositionA, a.getPosition(), 0.001);
            assertEquals("wrong parked position - b", parkPositionB, b.getPosition(), 0.001);
            assertEquals("wrong parked position - c", parkPositionM, c.getPosition(), 0.001);
            assertEquals("wrong parked position - m", parkPositionM, m.getPosition(), 0.001);

        } finally {
            ThreadContext.pop();
        }
    }
}
