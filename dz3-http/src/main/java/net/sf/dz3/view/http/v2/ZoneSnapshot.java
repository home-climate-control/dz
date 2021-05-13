package net.sf.dz3.view.http.v2;

import java.io.Serializable;

import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.ZoneState;

/**
 * Zone snapshot.
 * 
 * This class is common across the DZ server application, Home Climate Control proxy, wherever it is running,
 * and Home Climate Control Remote Android application (https://play.google.com/store/apps/details?id=com.homeclimatecontrol.dz3.view.android).
 * Hence, a strange "public" implementation - it needs to support easy JSON, XML or other serialization/deserialization.
 *
 * NOTE: The deviation* variables support fast "return to schedule" function on remote devices (otherwise, it'll take 
 * the full round trip time for them to reflect proper values, oscillating feedback loop becomes possible).
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2013
 */
public class ZoneSnapshot implements Comparable<ZoneSnapshot>, Serializable {

    private static final long serialVersionUID = 1968013529080011937L;

    public long timestamp;
    public String name;
    public HvacMode mode;
    public ZoneState state;
    public double signal;
    public double currentTemperature;
    public double setpointTemperature;
    public boolean enabled;
    public boolean onHold;
    public boolean voting;
    public String periodName;
    
    /**
     * How much does the {@link #setpointTemperature} differ from the schedule setpoint.
     */
    public double deviationSetpoint;
    
    /**
     * {@code true} if the scheduled 'enabled' value differs from current.
     */
    public boolean deviationEnabled;
    
    /**
     * {@code true} if the scheduled 'voting' value differs from current.
     */
    public boolean deviationVoting;
    
    public String error;

    /**
     * Useless, but mandatory constructor.
     */
    @SuppressWarnings("unused")
    private ZoneSnapshot() {
        
    }
    
    /**
     * Create an instance.
     * 
     * @param timestamp Time when this snapshot was created.
     * @param name Zone name.
     * @param mode Zone HVAC mode.
     * @param state Zone state.
     * @param signal Current thermostat signal.
     * @param currentTemperature Current zone temperature.
     * @param setpointTemperature Setpoint temperature.
     * @param enabled Whether the zone is enabled.
     * @param onHold Whether the zone is on hold.
     * @param voting Whether the zone is voting.
     * @param periodName Schedule period name currently running, or {@code null} if none.
     * @param onSchedule Whether the zone is on schedule ({@code true}) or settings are
     * altered ({@code false}).
     * @param
     * @param error Error message or {@code null} if no error condition exists.
     */
    public ZoneSnapshot(long timestamp, String name, HvacMode mode, ZoneState state,
            double signal, double currentTemperature,
            double setpointTemperature, boolean enabled, boolean onHold, boolean voting,
            String periodName,
            double deviationSetpoint,
            boolean deviationEnabled,
            boolean deviationVoting,
            String error) {

        this.timestamp = timestamp;
        this.name = name;
        this.mode = mode;
        this.state = state;
        this.signal = signal;
        this.currentTemperature = currentTemperature;
        this.setpointTemperature = setpointTemperature;
        this.enabled = enabled;
        this.onHold = onHold;
        this.voting = voting;
        this.periodName = periodName;
        this.deviationSetpoint = deviationSetpoint;
        this.deviationEnabled = deviationEnabled;
        this.deviationVoting = deviationVoting;
        this.error = error;
    }

    @Override
    public int compareTo(ZoneSnapshot o) {

//    	org.apache.log4j.Logger.getLogger(getClass()).error("ZoneSnapshot#toString", new IllegalStateException("Trace"));
    	
    	String us = Long.toString(timestamp) + "." + this;
    	String them = Long.toString(o.timestamp) + "." + o;
    	
        return us.compareTo(them);
    }

    @Override
    public boolean equals(Object o) {
        
        if (o == null) {
            return false;
        }
        
        return toString().equals(o.toString());
    }
    
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(name).append(": ").append(mode);
        sb.append(", ").append(state);
        sb.append(", signal=").append(signal);
        sb.append(", current=").append(currentTemperature);
        sb.append(", setpoint=").append(setpointTemperature);
        sb.append(enabled ? "" : ", OFF");
        sb.append(onHold ? ", on hold" : "");
        sb.append(voting ? "" : ", not voting");

        if (periodName != null) {
            sb.append(", period=").append(periodName);
        }

        sb.append(Double.compare(deviationSetpoint, 0d) != 0 ? ", setpoint differs from schedule by " + deviationSetpoint : "");

        sb.append(deviationEnabled ? ", 'enabled' differs from schedule" : "");
        sb.append(deviationVoting ? ", 'voting' differs from schedule" : "");

        if (error != null) {
            sb.append(", error=").append(error);
        }

        return sb.toString();
    }
}
