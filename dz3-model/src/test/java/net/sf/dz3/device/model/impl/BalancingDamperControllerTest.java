package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import com.homeclimatecontrol.jukebox.sem.ACT;
import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.actuator.Damper;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.UnitSignal;
import net.sf.dz3.device.sensor.impl.NullSensor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class BalancingDamperControllerTest {
    
    /**
     * Make sure that thermostats with negative demand don't cause damper control signals
     * out of acceptable range.
     */
    @Test
    public void testBoundaries() {
        
        Thermostat ts1 = new ThermostatModel("ts1", new NullSensor("address1", 0), new SimplePidController(20, 1, 0, 0, 0));
        Thermostat ts2 = new ThermostatModel("ts2", new NullSensor("address2", 0), new SimplePidController(20, 1, 0, 0, 0));
        
        Damper d1 = new DummyDamper("d1");
        Damper d2 = new DummyDamper("d2");
        
        BalancingDamperController damperController = new BalancingDamperController();
        
        damperController.put(ts1, d1);
        damperController.put(ts2, d2);
        
        long timestamp = 0;
        
        damperController.stateChanged(ts1, new ThermostatSignal(true, false, true, true, new DataSample<Double>(timestamp, "ts1", "ts1", 50.0, null)));
        damperController.stateChanged(ts2, new ThermostatSignal(true, false, true, true, new DataSample<Double>(timestamp, "ts2", "ts2", -50.0, null)));
    }
    
    /**
     * Make sure that zero demand from all thermostats doesn't cause NaN sent to dampers.
     */
    @Test
    public void testNaN() {
        
        Thermostat ts1 = new ThermostatModel("ts1", new NullSensor("address1", 0), new SimplePidController(20, 1, 0, 0, 0));
        
        DummyDamper d1 = new DummyDamper("d1");
        
        BalancingDamperController damperController = new BalancingDamperController();
        
        damperController.put(ts1, d1);
        
        // No calculations are performed unless the HVAC unit signal is present
        damperController.consume(new DataSample<UnitSignal>("unit1", "unit1", new UnitSignal(1.0, true, 0), null));
        
        damperController.stateChanged(ts1, new ThermostatSignal(true, false, true, true, new DataSample<Double>("ts1", "ts1", -50.0, null)));
        
        assertThat(d1.get()).as("damper position").isEqualTo(0.0);
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
        public double getPosition() throws IOException {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public ACT park() {
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public void set(double position) throws IOException {

            assertThat(position).isNotEqualTo(Double.NaN);
            assertThat(position).isLessThanOrEqualTo(1.0);
            assertThat(position).isGreaterThanOrEqualTo(0.0);

            currentPosition = position;
        }
        
        public double get() {
            
            if (currentPosition == null) {
                throw new IllegalStateException("Attempt to get a position that wasn never set");
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
