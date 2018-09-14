package net.sf.dz3.runtime;

import org.apache.logging.log4j.LogManager;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxDescriptor;

public class NativeSensorFactory implements SensorFactory<NativeSensorDescriptor> {

    @Override
    public AnalogSensor getSensor(final NativeSensorDescriptor descriptor) {

        LogManager.getLogger(getClass()).info("getSensor(" + descriptor + ") invoked");

        return new NativeSensor(descriptor);
    }

    public static class NativeSensor implements AnalogSensor {

        private final NativeSensorDescriptor descriptor;

        public NativeSensor(NativeSensorDescriptor descriptor) {
            this.descriptor = descriptor;
        }


        @Override
        public void addConsumer(DataSink<Double> consumer) {

            throw new IllegalStateException("Not Implemented");
        }

        @Override
        public void removeConsumer(DataSink<Double> consumer) {

            throw new IllegalStateException("Not Implemented");

        }

        @Override
        public JmxDescriptor getJmxDescriptor() {

            throw new IllegalStateException("Not Implemented");
        }

        @Override
        public String getAddress() {

            throw new IllegalStateException("Not Implemented");
        }

        @Override
        public DataSample<Double> getSignal() {

            throw new IllegalStateException("Not Implemented");
        }

        public String toString() {

            return "Sensor[" + descriptor + "]";
        }
    };
}
