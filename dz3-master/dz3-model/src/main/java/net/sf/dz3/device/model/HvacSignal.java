package net.sf.dz3.device.model;

import org.apache.log4j.Logger;

public class HvacSignal extends UnitSignal {

    private static final long serialVersionUID = -5975023170950869702L;

    public final HvacMode mode;

    public HvacSignal(HvacMode mode, double demand, boolean running, long uptime) {
        super(demand, running, uptime);
        
        this.mode = mode;

        if (mode.equals(HvacMode.OFF) && (demand > 0 || running)) {
            // That's a bit harsh at this point
            //throw new IllegalArgumentException("Invalid combination of (mode, demand, running): " + toString());
            Logger.getLogger(getClass()).warn("HvacSignal(): Invalid combination of (mode, demand, running): " + toString());
        }
    }

    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("(mode=").append(mode);
        sb.append(", demand=").append(demand);
        sb.append(", ").append(running ? "running" : "off");
        sb.append(", uptime=").append(uptime);
        sb.append(")");
        
        return sb.toString();
    }
}
