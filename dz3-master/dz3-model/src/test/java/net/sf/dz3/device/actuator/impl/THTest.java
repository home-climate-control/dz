package net.sf.dz3.device.actuator.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Ignore;
import org.junit.Test;

import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.UnitSignal;
import net.sf.dz3.device.model.impl.AbstractDamperController;
import net.sf.dz3.device.model.impl.BalancingDamperController;
import net.sf.dz3.device.model.impl.SimpleDamperController;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.NullSwitch;
import net.sf.dz3.instrumentation.Marker;
import net.sf.jukebox.datastream.signal.model.DataSample;

/**
 * Set of test cases to replicate https://github.com/home-climate-control/dz/issues/130.
 */
public class THTest {

    private final Logger logger = LogManager.getLogger(getClass());
    private final Random rg = new Random();

    private static final String WRONG_POSITION = "wrong position";
    private static final String WRONG_STATE = "wrong switch state";

    @Test
    public void testSyncFastSimple()
            throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        testSync("fast/simple", SimpleDamperController.class, 0, 0);
    }

    /**
     * VT: NOTE: This test may take up to 8+ seconds - too slow for development work. Enable if you need it.
     */
    @Ignore
    @Test
    public void testSyncSlowSimple()
            throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        testSync("slow/simple", SimpleDamperController.class, 100, 500);
    }

    @Test
    public void testSyncFastBalancing()
            throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        testSync("fast/balancing", BalancingDamperController.class, 0, 0);
    }

    /**
     * VT: NOTE: This test may take up to 8+ seconds - too slow for development work. Enable if you need it.
     */
    @Ignore
    @Test
    public void testSyncSlowBalancing()
            throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        testSync("slow/balancing", BalancingDamperController.class, 100, 500);
    }

    private void testSync(String marker, Class<? extends AbstractDamperController> controllerClass, long minDelay, int maxDelay)
            throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Marker m = new Marker(marker);
        ThreadContext.push(marker);

        try {

            ThermostatModel tsLivingRoom = mock(ThermostatModel.class);
            ThermostatModel tsKitchen = mock(ThermostatModel.class);
            ThermostatModel tsWestBathroom = mock(ThermostatModel.class);
            ThermostatModel tsWest = mock(ThermostatModel.class);

            doReturn("thermostat-livingroom").when(tsLivingRoom).getName();
            doReturn("thermostat-kitchen").when(tsKitchen).getName();
            doReturn("thermostat-westbathroom").when(tsWestBathroom).getName();
            doReturn("thermostat-west").when(tsWest).getName();

            Object lock = "lock";

            Switch switchLivingRoom = new NullSwitch("switch_livingroom_damper", minDelay, maxDelay, lock);
            Switch switchKitchen = new NullSwitch("switch_kitchen_damper", minDelay, maxDelay, lock);
            Switch switchWestBathroom = new NullSwitch("switch_westbathroom_damper", minDelay, maxDelay, lock);
            Switch switchWestDamper = new NullSwitch("switch_west_damper", minDelay, maxDelay, lock);
            Switch switchWestBoosterFan = new NullSwitch("switch_west_boosterfan", minDelay, maxDelay, lock);

            Set<Switch> switches = new LinkedHashSet<>();

            switches.add(switchLivingRoom);
            switches.add(switchKitchen);
            switches.add(switchWestBathroom);
            switches.add(switchWestDamper);
            switches.add(switchWestBoosterFan);

            Damper damperLivingRoom = new SwitchDamper("damper_livingroom", switchLivingRoom, 0.8, 1.0, 10);
            Damper damperKitchen = new SwitchDamper("damper_kitchen", switchKitchen, 0.8, 1.0, 10);
            Damper damperWestBathroom = new SwitchDamper("damper_westbathroom", switchWestBathroom, 0.8, 1.0, 10);

            Damper damperWest = new SwitchDamper("damper_west", switchWestDamper, 0.8, 1.0, 10);
            Damper damperWestBoosterFan = new SwitchDamper("damper_westboosterfan", switchWestBoosterFan, 0.8, 0.0, 10, true);

            Set<Damper> west = new LinkedHashSet<>();

            west.add(damperWest);
            west.add(damperWestBoosterFan);

            Damper damperMultiplexerWest = new DamperMultiplexer("damper_multiplexer_west", west);

            Set<Damper> dampers = new LinkedHashSet<>();

            dampers.add(damperLivingRoom);
            dampers.add(damperKitchen);
            dampers.add(damperWestBathroom);
            dampers.add(damperWest);
            dampers.add(damperWestBoosterFan);
            dampers.add(damperMultiplexerWest);

            // TreeMap will not work here because of the way Mockito works
            // Note 'Thermostat' here vs. 'ThermostatModel' for the mock
            Map<Thermostat, Damper> ts2damper = new LinkedHashMap<>();

            ts2damper.put(tsLivingRoom, damperLivingRoom);
            ts2damper.put(tsKitchen, damperKitchen);
            ts2damper.put(tsWestBathroom, damperWestBathroom);
            ts2damper.put(tsWest, damperMultiplexerWest);

            Unit u = mock(Unit.class);

            Constructor<? extends AbstractDamperController> c = controllerClass.getDeclaredConstructor(Unit.class, Map.class);
            AbstractDamperController dc = c.newInstance(u, ts2damper);

            logger.info("Damper map: {}", Arrays.asList(dc.getDamperMap()));

            // VT: NOTE: It may be a better idea to inject fixed time; let's see if this works
            long timestamp = System.currentTimeMillis();

            // This will wait until all the movements are complete - unlike real life scenario;
            // that'll come later

            dc.stateChanged(tsWest, new ThermostatSignal(
                    true, false, true, true,
                    new DataSample<Double>(timestamp, "sensor_west", "sensor_west", 3.0625, null))).get();

            // The unit is off, dampers are parked

            logStatus(dampers, switches);

            assertEquals(WRONG_POSITION, damperLivingRoom.getParkPosition(), damperLivingRoom.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperKitchen.getParkPosition(), damperKitchen.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperWestBathroom.getParkPosition(), damperWestBathroom.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperMultiplexerWest.getParkPosition(), damperMultiplexerWest.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperWest.getParkPosition(), damperWest.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperWestBoosterFan.getParkPosition(), damperWestBoosterFan.getPosition(), 0.0001);

            assertEquals(WRONG_STATE, true, switchLivingRoom.getState());
            assertEquals(WRONG_STATE, true, switchKitchen.getState());
            assertEquals(WRONG_STATE, true, switchWestBathroom.getState());
            assertEquals(WRONG_STATE, true, switchWestDamper.getState());
            assertEquals(WRONG_STATE, true, switchWestBoosterFan.getState());

            // The above stateChanged() also changed the state of the Unit to "running",
            // next stateChanged() will be handled differently

            // For a good measure, let's advance the timestamp between signals
            timestamp += 50 + rg.nextInt(100);
            dc.consume(new DataSample<UnitSignal>(timestamp, "unit", "unit", new UnitSignal(0, true, 0), null));

            timestamp += 50 + rg.nextInt(100);
            dc.consume(new DataSample<UnitSignal>(timestamp, "unit", "unit", new UnitSignal(3.0625, true, 0), null));

            timestamp += 50 + rg.nextInt(100);
            dc.consume(new DataSample<UnitSignal>(timestamp, "unit", "unit", new UnitSignal(6.875, true, 0), null));

            timestamp += 50 + rg.nextInt(100);
            dc.consume(new DataSample<UnitSignal>(timestamp, "unit", "unit", new UnitSignal(10.3125, true, 0), null));

            timestamp += 50 + rg.nextInt(100);
            dc.consume(new DataSample<UnitSignal>(timestamp, "unit", "unit", new UnitSignal(13.6875, true, 0), null));


            // After that, the demand rises by small increments until the whole thing blows up
            // The count in the crash log is 9, let's make sure it's exceeded

            double demand = 13.6875;
            for (int count = 0; count < 50; count++) {

                timestamp += 50 + rg.nextInt(100);
                demand += rg.nextDouble()/10;

                dc.consume(new DataSample<UnitSignal>(timestamp, "unit", "unit", new UnitSignal(demand, true, 0), null));
            }

            {
                // VT: NOTE: This may need to be increased on a slow system to make the test
                // pass - but it's a temporary block anyway; it'll go away with the proper
                // implementation

                long delay = 1000;

                logger.warn("sleeping {}ms to let things settle down...", delay);
                Thread.sleep(delay);
                logger.warn("the sleep is over");
            }

            dc.powerOff().get();

            assertEquals(WRONG_POSITION, damperLivingRoom.getParkPosition(), damperLivingRoom.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperKitchen.getParkPosition(), damperKitchen.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperWestBathroom.getParkPosition(), damperWestBathroom.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperMultiplexerWest.getParkPosition(), damperMultiplexerWest.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperWest.getParkPosition(), damperWest.getPosition(), 0.0001);
            assertEquals(WRONG_POSITION, damperWestBoosterFan.getParkPosition(), damperWestBoosterFan.getPosition(), 0.0001);

            assertEquals(WRONG_STATE, true, switchLivingRoom.getState());
            assertEquals(WRONG_STATE, true, switchKitchen.getState());
            assertEquals(WRONG_STATE, true, switchWestBathroom.getState());
            assertEquals(WRONG_STATE, true, switchWestDamper.getState());
            assertEquals(WRONG_STATE, true, switchWestBoosterFan.getState());

            logger.info("Damper map: {}", Arrays.asList(dc.getDamperMap()));

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void logStatus(Set<Damper> dampers, Set<Switch> switches) {
        ThreadContext.push("position");

        dampers.stream().forEach(d -> {
            try {
                logger.info("{}: {}", d.getName(), d.getPosition());
            } catch (IOException ex) {
                // This damper won't throw it
            }
        });

        ThreadContext.pop();
        ThreadContext.push("state");

        switches.stream().forEach(s -> {
            try {
                logger.info("{}: {}", s.getAddress(), s.getState());
            } catch (IOException ex) {
                // This damper won't throw it
            }
        });

        ThreadContext.pop();
    }

}
