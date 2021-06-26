package net.sf.dz3.view.webui.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.Addressable;
import net.sf.dz3.device.sensor.AnalogSensor;

/**
 * A static snapshot of an actual analog sensor.
 */
public class AnalogSensorSnapshot implements AnalogSensor {

    private final String address;
    private final DataSample<Double> signal;


    public AnalogSensorSnapshot(AnalogSensor template) {
        this.address = template.getAddress();
        this.signal = template.getSignal();
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public DataSample<Double> getSignal() {
        return signal;
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {
        throw new UnsupportedOperationException();
    }

    @JsonIgnore
    @Override
    public JmxDescriptor getJmxDescriptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Addressable o) {
        // Can't afford to collide with the wrapper
        return (getClass().getName() + getAddress()).compareTo((o.getClass().getName() + o.getAddress()));
    }
}
