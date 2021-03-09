package net.sf.dz3.view.http.v1;

import java.util.Map;
import java.util.TreeMap;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.http.common.QueueFeeder;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
 * Keeps {@link #consume(DataSample) receiving} data notifications and
 * {@link #emit(UpstreamBlock) stuffing} them into the queue. 
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public class SensorRenderer extends QueueFeeder<UpstreamBlock> implements DataSink<Double>, RestRenderer<DataSample<Double>> {

    private final AnalogSensor source;
    
    public SensorRenderer(AnalogSensor source, Map<String, Object> context) {
        
        super(context);
        
        this.source = source;

        source.addConsumer(this);
    }

    @Override
    public String getPath() {

        return "sensor/" + source.getAddress();
    }

    @Override
    public Map<String, String> getState(DataSample<Double> signal) {

        Map<String, String> stateMap = new TreeMap<String, String>();
        
        stateMap.put("timestamp", Long.toString(signal.timestamp));
        
        if (!signal.isError()) {

            stateMap.put("signal", Double.toString(signal.sample));
        
        } else {
            
            // VT: FIXME: This may be an oversimplification
            stateMap.put("error", signal.error.getMessage());
        }
        
        return stateMap;
    }

    @Override
    public void consume(DataSample<Double> signal) {
        
        emit(new UpstreamBlock(getPath(), getState(signal)));
    }
}
