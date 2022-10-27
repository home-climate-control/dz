package net.sf.dz3r.signal.hvac;

/**
 * Calling status.
 *
 * This object defines the actual thermostat or economizer status in real time.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2022
 */
public class CallingStatus {

    /**
     * Raw component of the hysteresis controller. Here for instrumentation purposes only.
     */
    public final Double sample;

    public final double demand;
    public final boolean calling;

    public CallingStatus(Double sample, double demand, boolean calling) {
        this.sample = sample;
        this.demand = demand;
        this.calling = calling;
    }

    @Override
    public String toString() {
        return "{sample=" + sample + ",demand=" + demand + ", calling=" + calling + "}";
    }
}
