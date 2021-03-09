package net.sf.dz3.view.http.v2;

import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.joda.time.DateTime;

import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneState;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.scheduler.Period;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.scheduler.Scheduler.Deviation;
import net.sf.dz3.view.http.common.QueueFeeder;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
/**
 * Keeps {@link #consume(DataSample) receiving} data notifications and
 * {@link #emit(ZoneSnapshot) stuffing} them into the queue. 
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */

public class ThermostatRenderer extends QueueFeeder<ZoneSnapshot> implements DataSink<ThermostatSignal>, JsonRenderer<DataSample<ThermostatSignal>> {

    private final ThermostatModel source;
    private final Scheduler scheduler;
    
    public ThermostatRenderer(ThermostatModel source, Map<String, Object> context, Scheduler scheduler) {
        
        super(context);

        this.source = source;
        this.scheduler = scheduler;

        // VT: NOTE: This is a simplification sufficient until the server side framework
        // is ironed out. Later, the controller signal needs to be added, just like it is
        // done in ThermostatPanel.
        
        source.addConsumer(this);
    }

    /**
     * 
     * @param signal Signal to render.
     * @return Map containing the signal state.
     * 
     * @deprecated It is now forgotten how this got here and what for.
     */
    @Deprecated
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
        
        String name = source.getName();
        // VT: FIXME: Is it even possible to use anything other than PID controller?
        HvacMode mode = ((AbstractPidController) source.getController()).getP() > 0 ? HvacMode.COOLING : HvacMode.HEATING;
        ZoneState state = signal.isError() ? ZoneState.ERROR : (signal.sample.enabled ? (signal.sample.calling ? ZoneState.CALLING : ZoneState.HAPPY) : ZoneState.OFF);
        double thermostatSignal = signal.sample.demand.sample;

        double currentTemperature;
        
        DataSample<Double> sample = source.getSensor().getSignal();
        
        // It would be a good idea to use Double.NaN for null and error cases. However,
        // this would later violate JSON specification, so let's leave this at 0 and see how it works.
        
        if (sample == null) {
            
            LogManager.getLogger(getClass()).debug("current=null");
            currentTemperature = 0;
            
        } else if ( sample.isError() ) {
            
            LogManager.getLogger(getClass()).debug("current=error");
            currentTemperature = 0;
        
        } else {
            
            currentTemperature = sample.sample;
        }
        
        double setpointTemperature = source.getController().getSetpoint();
        boolean enabled = signal.sample.enabled;
        boolean onHold = signal.sample.onHold;
        boolean voting = signal.sample.voting;
        String error = signal.isError() ? signal.error.getMessage() : null;
        
        String periodName = getPeriod();
        
        Deviation deviation = scheduler == null ? new Deviation(0, false, false) : scheduler.getDeviation(source, setpointTemperature, enabled, voting, new DateTime(signal.timestamp));
            
        emit(new ZoneSnapshot(signal.timestamp, name, mode, state, thermostatSignal, currentTemperature,
                setpointTemperature, enabled, onHold, voting, periodName, 
                deviation.setpoint, deviation.enabled, deviation.voting,
                error));
    }

    /**
     * Find out the name of the current period for the zone served by this renderer.
     * 
     * @return {@code null} if there's no scheduler, a predefined string if there's no period, or a period name.
     */
    private String getPeriod() {
        
        if (scheduler == null) {
            return null;
        }

        Period p = scheduler.getCurrentPeriod(source);
        
        if (p == null) {
            return "(no period is active)";
        }
        
        return p.name;
    }

    @Override
    public String render(Object source) {
        
        return source.toString();
    }
}

