package net.sf.dz3.view.http.v1;

import java.util.Map;
import java.util.TreeMap;

import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.view.http.common.QueueFeeder;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
/**
 * Keeps {@link #consume(DataSample) receiving} data notifications and
 * {@link #emit(UpstreamBlock) stuffing} them into the queue. 
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */

public class ThermostatRenderer extends QueueFeeder<UpstreamBlock> implements DataSink<ThermostatSignal>, RestRenderer<DataSample<ThermostatSignal>> {

    private final ThermostatModel source;
    
    public ThermostatRenderer(ThermostatModel source, Map<String, Object> context) {
        
        super(context);

        this.source = source;

        // VT: NOTE: This is a simplification sufficient until the server side framework
        // is ironed out. Later, the controller signal needs to be added, just like it is
        // done in ThermostatPanel.
        
        source.addConsumer(this);
    }

    @Override
    public String getPath() {
        
        return "thermostat/" + source.getName();
    }

    @Override
    public Map<String, String> getState(DataSample<ThermostatSignal> signal) {
        
        Map<String, String> stateMap = new TreeMap<String, String>();
        
        stateMap.put("timestamp", Long.toString(signal.timestamp));
        
        if (!signal.isError()) {

            stateMap.put("calling", Integer.toString(signal.sample.calling ? 1 : 0));
            stateMap.put("voting", Integer.toString(signal.sample.voting ? 1 : 0));
            stateMap.put("demand", Double.toString(signal.sample.demand.sample));
        
        } else {
            
            // VT: FIXME: This may be an oversimplification
            stateMap.put("error", signal.error.getMessage());
        }
        
        // VT: NOTE: This is not quite nice because it bypasses the time stream,
        // need to think about augmenting the thermostat signal with it
        
        stateMap.put("on", Integer.toString(source.isOn() ? 1 : 0));
        stateMap.put("hold", Integer.toString(source.isOnHold() ? 1 : 0));
        
        return stateMap;
    }

    @Override
    public void consume(DataSample<ThermostatSignal> signal) {
        
        emit(new UpstreamBlock(getPath(), getState(signal)));
    }
}
