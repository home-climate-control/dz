package net.sf.dz3r.signal;

/**
 * Thermostat status.
 *
 * This object defines the actual thermostat status in real time.
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
