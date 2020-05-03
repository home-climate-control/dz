package net.sf.dz3.device.actuator.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.impl.BalancingDamperController;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.NullSwitch;
import net.sf.jukebox.datastream.signal.model.DataSample;

/**
 * Set of test cases to replicate https://github.com/home-climate-control/dz/issues/130.
 */
public class THTest {

    private static final String WRONG_POSITION = "wrong position";

    @Test
    public void testSync() throws InterruptedException, ExecutionException, IOException {

        ThermostatModel tsLivingRoom = mock(ThermostatModel.class);
        ThermostatModel tsKitchen = mock(ThermostatModel.class);
        ThermostatModel tsWestBathroom = mock(ThermostatModel.class);
        ThermostatModel tsWest = mock(ThermostatModel.class);

        doReturn("thermostat-livingroom").when(tsLivingRoom).getName();
        doReturn("thermostat-kitchen").when(tsKitchen).getName();
        doReturn("thermostat-westbathroom").when(tsWestBathroom).getName();
        doReturn("thermostat-west").when(tsWest).getName();

        Switch switchLivingRoom = new NullSwitch("switch_livingroom_damper");
        Switch switchKitchen = new NullSwitch("switch_kitchen_damper");
        Switch switchWestBathroom = new NullSwitch("switch_westbathroom_damper");
        Switch switchWestDamper = new NullSwitch("switch_west_damper");
        Switch switchWestBoosterFan = new NullSwitch("switch_west_boosterfan");

        Damper damperLivingRoom = new SwitchDamper("damper_livingroom", switchLivingRoom, 0.8, 1.0);
        Damper damperKitchen = new SwitchDamper("damper_kitchen", switchKitchen, 0.8, 1.0);
        Damper damperWestBathroom = new SwitchDamper("damper_westbathroom", switchWestBathroom, 0.8, 1.0);

        Damper damperWest = new SwitchDamper("damper_west", switchWestDamper, 0.8, 1.0);
        Damper damperWestBoosterFan = new SwitchDamper("damper_westboosterfan", switchWestBoosterFan, 0.8, 1.0, true);

        Set<Damper> west = new LinkedHashSet<>();

        west.add(damperWest);
        west.add(damperWestBoosterFan);

        Damper damperMultiplexerWest = new DamperMultiplexer("damper_multiplexer_west", west);

        // TreeMap will not work here because of the way Mockito works
        // Note 'Thermostat' here vs. 'ThermostatModel' for the mock
        Map<Thermostat, Damper> ts2damper = new LinkedHashMap<>();

        ts2damper.put(tsLivingRoom, damperLivingRoom);
        ts2damper.put(tsKitchen, damperKitchen);
        ts2damper.put(tsWestBathroom, damperWestBathroom);
        ts2damper.put(tsWest, damperMultiplexerWest);

        Unit u = mock(Unit.class);
        BalancingDamperController dc = new BalancingDamperController(u, ts2damper);

        // VT: NOTE: It may be a better idea to inject fixed time; let's see if this works
        long timestamp = System.currentTimeMillis();

        // This will wait until all the movements are complete - unlike real life scenario;
        // that'll come later

        dc.stateChanged(tsWest, new ThermostatSignal(
                true, false, true, true,
                new DataSample<Double>(timestamp, "sensor_west", "sensor_west", 3.0625, null))).get();

        // The unit is off, dampers are parked

        assertEquals(WRONG_POSITION, damperLivingRoom.getParkPosition(), damperLivingRoom.getPosition(), 0.0001);
        assertEquals(WRONG_POSITION, damperKitchen.getParkPosition(), damperKitchen.getPosition(), 0.0001);
        assertEquals(WRONG_POSITION, damperWestBathroom.getParkPosition(), damperWestBathroom.getPosition(), 0.0001);
        assertEquals(WRONG_POSITION, damperMultiplexerWest.getParkPosition(), damperMultiplexerWest.getPosition(), 0.0001);
        assertEquals(WRONG_POSITION, damperWest.getParkPosition(), damperWest.getPosition(), 0.0001);
        assertEquals(WRONG_POSITION, damperWestBoosterFan.getParkPosition(), damperWestBoosterFan.getPosition(), 0.0001);

        // To be continued...
    }

}
