package net.sf.dz3.device.sensor.impl.onewire;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.SensorDeviceContainer;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

import com.dalsemi.onewire.container.OneWireContainer;

/**
 * 1-Wire device that is a single channel signal sensor.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2000-2012
 */
abstract public class AbstractSensorContainer extends OneWireDeviceContainer implements SensorDeviceContainer<AnalogSensor>, AnalogSensor {
    
    protected DataSample<Double> lastKnownSignal = new DataSample<Double>(System.currentTimeMillis(), getAddress(), getSignature(), null, new IllegalStateException("Just Started"));

    public AbstractSensorContainer(OneWireContainer container) {
        super(container);
    }

    protected final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();
    
    public void stateChanged(Double signal, Throwable error) {
        DataSample<Double> sample = new DataSample<Double>(System.currentTimeMillis(), getAddress(), getSignature(), signal, error);
        dataBroadcaster.broadcast(sample);
        
        lastKnownSignal = sample;
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {
        
        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {
        
        dataBroadcaster.removeConsumer(consumer);
    }

    @Override
    public DataSample<Double> getSignal() {
        
        return lastKnownSignal;
    }
}
