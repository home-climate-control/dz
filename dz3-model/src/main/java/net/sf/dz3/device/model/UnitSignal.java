package net.sf.dz3.device.model;

import java.io.Serializable;

/**
 * Representation of HVAC state in a form suitable for consumption by signal listeners.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2010
 */
public class UnitSignal implements Serializable {

    private static final long serialVersionUID = -8751463335825205628L;
    
    public final double demand;
    public final boolean running;
    public final long uptime;
    
    public UnitSignal(double demand, boolean running, long uptime) {
        
        this.demand = demand;
        this.running = running;
        this.uptime = uptime;
        
        if (!running && uptime > 0) {
            throw new IllegalArgumentException("non-zero uptime (" + uptime + ") and not running?");
        }
        
        // VT: NOTE: May need to check the reverse condition: (running && uptime == 0).
        //
        // May be a bit dangerousbecause running must be set to true and immediately
        // passed down with uptime still zero
    }
    
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("(demand=").append(demand);
        sb.append(", ").append(running ? "running" : "off");
        sb.append(", uptime=").append(uptime);
        sb.append(")");
        
        return sb.toString();
    }
}
