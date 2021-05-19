package net.sf.dz3.device.model;

import java.io.Serializable;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;

/**
 * Simplified version of {@link ThermosatStatus} intended for {@link ZoneController} consumption.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2010
 */
public class ThermostatSignal implements Serializable {

    private static final long serialVersionUID = -3821345385714321362L;

    public final boolean enabled;
    public final boolean onHold;
    public final boolean calling;
    public final boolean voting;
    public final DataSample<Double> demand;
    
    public ThermostatSignal(boolean enabled, boolean onHold, boolean calling, boolean voting, DataSample<Double> demand) {
	
	if (demand == null) {
	    throw new IllegalArgumentException("demand can't be null");
	}

	this.enabled = enabled;
	this.onHold = onHold;
	this.calling = calling;
	this.voting = voting;
	this.demand = demand;
    }
    
    @Override
    public String toString() {
	
	StringBuilder sb = new StringBuilder();
	
	sb.append("(");
        sb.append(enabled ? "+" : "-");
        sb.append(onHold ? "H" : ".");
	sb.append(calling ? "C" : ".");
	sb.append(voting ? "V" : ".");
	sb.append(", ").append(demand).append(")");
	
	return sb.toString();
    }
}
