package net.sf.dz3.device.model.impl;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import junit.framework.TestCase;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.UnitSignal;
import net.sf.dz3.device.sensor.impl.NullSensor;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.servomaster.device.model.TransitionStatus;

public class BalancingDamperControllerTest extends TestCase {
    
    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Make sure that thermostats with negative demand don't cause damper control signals
     * out of acceptable range.
     */
    public void testBoundaries() {
        
        ThreadContext.push("testBoundaries");
        
        try {

            Thermostat ts1 = new ThermostatModel("ts1", new NullSensor("address1", 0), new SimplePidController(20, 1, 0, 0, 0));
            Thermostat ts2 = new ThermostatModel("ts2", new NullSensor("address2", 0), new SimplePidController(20, 1, 0, 0, 0));

            Damper d1 = new DummyDamper("d1");
            Damper d2 = new DummyDamper("d2");

            Unit u = mock(Unit.class);
            BalancingDamperController damperController = new BalancingDamperController(u, new HashMap<Thermostat, Damper>());

            damperController.put(ts1, d1);
            damperController.put(ts2, d2);

            long timestamp = 0;

            damperController.stateChanged(ts1, new ThermostatSignal(true, false, true, true, new DataSample<Double>(timestamp, "ts1", "ts1", 50.0, null)));
            damperController.stateChanged(ts2, new ThermostatSignal(true, false, true, true, new DataSample<Double>(timestamp, "ts2", "ts2", -50.0, null)));

        } finally {

            ThreadContext.pop();
        }
    }
    
    /**
     * Make sure that zero demand from all thermostats doesn't cause NaN sent to dampers.
     */
    public void testNaN() throws InterruptedException, ExecutionException {
        
        ThreadContext.push("testNaN");
        
        try {

            Thermostat ts1 = new ThermostatModel("ts1", new NullSensor("address1", 0), new SimplePidController(20, 1, 0, 0, 0));

            DummyDamper d1 = new DummyDamper("d1");

            Unit u = mock(Unit.class);
            BalancingDamperController damperController = new BalancingDamperController(u, new HashMap<Thermostat, Damper>());

            damperController.put(ts1, d1);

            // No calculations are performed unless the HVAC unit signal is present
            damperController.consume(new DataSample<UnitSignal>("unit1", "unit1", new UnitSignal(1.0, true, 0), null));

            Future<TransitionStatus> done = damperController.stateChanged(ts1, new ThermostatSignal(true, false, true, true, new DataSample<Double>("ts1", "ts1", -50.0, null)));

            TransitionStatus status = done.get();

            assertTrue(status.isOK());

            damperController.powerOff().get();

            assertEquals("Wrong damper position", d1.getParkPosition(), d1.get(), 0.000000000001);

        } finally {

            ThreadContext.pop();
        }
    }

    /**
     * Make sure that dampers are correctly parked on power off.
     */
    public void testPowerOff() throws InterruptedException, ExecutionException {

        ThreadContext.push("testPowerOff");

        try {

            Thermostat ts1 = new ThermostatModel("ts1", new NullSensor("address1", 0), new SimplePidController(20, 1, 0, 0, 0));

            DummyDamper d1 = new DummyDamper("d1");

            Unit u = mock(Unit.class);
            BalancingDamperController damperController = new BalancingDamperController(u, new HashMap<Thermostat, Damper>());

            damperController.put(ts1, d1);

            Future<TransitionStatus> done = damperController.powerOff();

            TransitionStatus status = done.get();

            assertTrue(status.isOK());

            // VT: NOTE: Need this because of asynchronous nature of damper transitions
            logger.debug("about to assert");

            assertEquals("Wrong damper position", 1.0, d1.get(), 0.000000000001);

        } finally {

            ThreadContext.pop();
        }
    }

    private static class DummyDamper implements Damper {
        
        private final String name;
        private Double currentPosition = null;
        
        public DummyDamper(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public double getParkPosition() {
            return 1.0;
        }

        @Override
        public boolean isCustomParkPosition() {
            return true;
        }

        @Override
        public double getPosition() throws IOException {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public Future<TransitionStatus> park() {

            currentPosition = getParkPosition();

            TransitionStatus status = new TransitionStatus(0);

            status.complete(0, null);

            return CompletableFuture.completedFuture(status);
        }

        @Override
        public Future<TransitionStatus> set(double position) {
            
            assertTrue("got NaN", Double.compare(position, Double.NaN) != 0);
            assertTrue("position is above 1.0: " + position, position <= 1.0);
            assertTrue("position is below 0.0: " + position, position >= 0.0);

            currentPosition = position;

            TransitionStatus status = new TransitionStatus(0);

            status.complete(0, null);

            return CompletableFuture.completedFuture(status);
        }
        
        public double get() {
            
            if (currentPosition == null) {
                throw new IllegalStateException("Attempt to get a position that was never set");
            }
            
            return currentPosition;
        }

        @Override
        public void setParkPosition(double throttle) {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public void consume(DataSample<Double> signal) {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public void addConsumer(DataSink<Double> consumer) {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public void removeConsumer(DataSink<Double> consumer) {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public JmxDescriptor getJmxDescriptor() {
            throw new UnsupportedOperationException("Not Implemented");
        }
    }
}
