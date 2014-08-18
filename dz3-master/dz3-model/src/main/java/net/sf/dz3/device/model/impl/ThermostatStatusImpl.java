package net.sf.dz3.device.model.impl;

import net.sf.dz3.device.model.ThermostatStatus;

public class ThermostatStatusImpl extends ZoneStatusImpl implements ThermostatStatus {
    
    private static final long serialVersionUID = -2031584478506738431L;
    
    public final double controlSignal;
    public final boolean hold;
    public final boolean error;
    
    ThermostatStatusImpl(
            double setpoint,
            double controlSignal,
            int dumpPriority,
            boolean enabled,
            boolean hold,
            boolean voting,
            boolean error) {
        
        super(setpoint, dumpPriority, enabled, voting);
        
        this.controlSignal = controlSignal;
        this.hold = hold;
        this.error = error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getControlSignal() {
        return controlSignal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnHold() {
        return hold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isError() {
        return error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("setpoint=").append(setpoint);
        sb.append(", controlSignal=").append(controlSignal);
        sb.append(enabled ? ", enabled" : ", disabled");
        sb.append(voting ? ", voting" : ", not voting");
        sb.append(error ? ", error" : ", ok");

        return sb.toString();
    }
}
