package net.sf.dz3.device.model;

import java.io.Serializable;

public enum HvacMode implements Comparable<HvacMode>, Serializable {

    COOLING(-1, "Cooling"),
    OFF(0, "Off"),
    HEATING(1, "Heating");

    public final int mode;
    public final String description;

    private HvacMode(int mode, String description) {
	this.mode = mode;
	this.description = description;
    }

    @Override
    public String toString() {
	return description;
    }
}
