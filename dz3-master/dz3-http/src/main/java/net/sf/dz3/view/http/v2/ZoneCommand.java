package net.sf.dz3.view.http.v2;

import java.io.Serializable;

public class ZoneCommand implements Comparable<ZoneCommand>, Serializable {

    private static final long serialVersionUID = -6584570502034039877L;
    
    public String name;
    public double setpointTemperature;
    public boolean enabled;
    public boolean onHold;
    public boolean voting;

    /**
     * Useless, but mandatory constructor.
     */
    @SuppressWarnings("unused")
    private ZoneCommand() {
        
    }
    
    /**
     * Create an instance.
     * 
     * @param name Zone name.
     * @param setpointTemperature Setpoint temperature.
     * @param enabled Whether the zone is enabled.
     * @param onHold Whether the zone is on hold.
     * @param voting Whether the zone is voting.
     */
    public ZoneCommand(String name, double setpointTemperature,
            boolean enabled, boolean onHold, boolean voting) {

        this.name = name;
        this.setpointTemperature = setpointTemperature;
        this.enabled = enabled;
        this.onHold = onHold;
        this.voting = voting;
    }

    @Override
    public int compareTo(ZoneCommand other) {

        return toString().compareTo(other.toString());
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

        sb.append(name).append(": setpoint=").append(setpointTemperature);
        sb.append(enabled ? "" : ", OFF");
        sb.append(onHold ? ", on hold" : "");
        sb.append(voting ? "" : ", not voting");

        return sb.toString();
    }

    public boolean matches(ZoneSnapshot sample) {
        
        return name.equals(sample.name)
            && Double.compare(setpointTemperature, sample.setpointTemperature) == 0
            && enabled == sample.enabled
            && onHold == sample.onHold
            && voting == sample.voting;
    }
}
