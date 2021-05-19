package net.sf.dz3.device.model.impl;

import java.io.Serializable;

import net.sf.dz3.device.model.ZoneStatus;

public class ZoneStatusImpl implements ZoneStatus, Serializable {

    private static final long serialVersionUID = 7691993493568700451L;
    public final double setpoint;
    public final int dumpPriority;
    public final boolean enabled;
    public final boolean voting;

    public ZoneStatusImpl(double setpoint, int dumpPriority, boolean enabled, boolean voting) {

        if (Double.compare(setpoint, Double.NaN) == 0 || setpoint == Double.NEGATIVE_INFINITY || setpoint == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("Invalid setpoint " + setpoint);
        }

        if (dumpPriority < 0) {

            throw new IllegalArgumentException("Dump priority must be non-negative (" + dumpPriority + " given)");
        }

        this.setpoint = setpoint;
        this.dumpPriority = dumpPriority;
        this.enabled = enabled;
        this.voting = voting;
    }

    @Override
    public double getSetpoint() {
        return setpoint;
    }

    @Override
    public int getDumpPriority() {
        return dumpPriority;
    }

    @Override
    public boolean isOn() {
        return enabled;
    }

    @Override
    public boolean isVoting() {
        return voting;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("setpoint=").append(setpoint);
        sb.append(enabled ? ", enabled" : ", disabled");
        sb.append(voting ? ", voting" : ", not voting");
        sb.append(dumpPriority != 0 ? ", dump priority=" + dumpPriority : "");

        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {

        if (other == null) {
            return false;
        }

        if (!(other instanceof ZoneStatus)) {
            return false;
        }

        ZoneStatus otherStatus = (ZoneStatus) other;

        return (Double.compare(getSetpoint(), otherStatus.getSetpoint()) == 0)
            && (getDumpPriority() == otherStatus.getDumpPriority())
            && (isOn() == otherStatus.isOn())
            && (isVoting() == otherStatus.isVoting());
    }

    @Override
    public int hashCode() {

        StringBuilder sb = new StringBuilder();

        return sb.append(setpoint).append(enabled).append(voting).append(dumpPriority).toString().hashCode();
    }
}
