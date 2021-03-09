package net.sf.dz3.view.mqtt.v1;

import java.io.StringWriter;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

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
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
/**
 * Keeps {@link #consume(DataSample) receiving} data notifications and
 * {@link #emit(ZoneSnapshot) stuffing} them into the queue. 
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2019
 */

public class ThermostatRenderer extends QueueFeeder<UpstreamBlock> implements DataSink<ThermostatSignal>, JsonRenderer<DataSample<ThermostatSignal>> {

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
     * VT: FIXME: Create {@code MqttRenderer}
     */
    private String getTopic() {
        return "thermostat/" + source.getName();
    }

    @Override
    public void consume(DataSample<ThermostatSignal> signal) {
        
        emit(new UpstreamBlock(getTopic(), render(signal)));
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
    public String render(DataSample<ThermostatSignal> signal) {
        
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
            
        JsonObjectBuilder b = Json.createObjectBuilder();

        b.add("entityType", MqttConnector.EntityType.THERMOSTAT.toString());
        b.add("timestamp", signal.timestamp);
        b.add("name", name);
        b.add("signature", signal.signature);
        b.add("mode", mode.description);
        b.add("state", state.toString());
        b.add("thermostatSignal", thermostatSignal);
        b.add("currentTemperature", currentTemperature);
        b.add("setpointTemperature", setpointTemperature);
        b.add("enabled", enabled);
        b.add("onHold", onHold);
        b.add("voting", voting);

        if (periodName != null) {
            b.add("periodName", periodName);
        }

        b.add("deviation.setpoint", deviation.setpoint);
        b.add("deviation.enabled", deviation.enabled);
        b.add("deviation.voting", deviation.voting);

        if (error != null) {
            b.add("error", error);
        }

        JsonObject message = b.build();
        StringWriter sw = new StringWriter();
        JsonWriter jw = Json.createWriter(sw);

        jw.writeObject(message);
        jw.close();

        return sw.toString();
    }
}
