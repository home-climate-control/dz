package net.sf.dz3.runtime;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.apache.logging.log4j.LogManager;

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
            // TODO Auto-generated method stub

        }

        @Override
        public void removeConsumer(DataSink<Double> consumer) {
            // TODO Auto-generated method stub

        }

        @Override
        public JmxDescriptor getJmxDescriptor() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getAddress() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DataSample<Double> getSignal() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String toString() {

            return "Sensor[" + descriptor + "]";
        }
    };
}
