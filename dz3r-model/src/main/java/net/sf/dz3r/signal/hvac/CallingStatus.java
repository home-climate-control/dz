package net.sf.dz3r.signal.hvac;

/**
 * Calling status.
 *
 * This object defines the actual thermostat or economizer status in real time.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2022
 */
public class CallingStatus {

    public final double demand;
    public final boolean calling;

    public CallingStatus(double demand, boolean calling) {
        this.demand = demand;
        this.calling = calling;
    }

    @Override
    public String toString() {
        return "{demand=" + demand + ", calling=" + calling + "}";
    }
}
