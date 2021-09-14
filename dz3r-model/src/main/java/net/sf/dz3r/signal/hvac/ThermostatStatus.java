package net.sf.dz3r.signal.hvac;

/**
 * Thermostat status.
 *
 * This object defines the actual thermostat status in real time.
 *
 * A conscious decision was made not to make it carry the current process variable (temperature) -
 * even though it complicates the signal processing chain (need to pass it elsewhere), it keeps unrelated
 * entities separate.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ThermostatStatus {

    public final double demand;
    public final boolean calling;

    public ThermostatStatus(double demand, boolean calling) {
        this.demand = demand;
        this.calling = calling;
    }

    @Override
    public String toString() {
        return "{demand=" + demand + ", calling=" + calling + "}";
    }
}
