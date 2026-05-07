package net.sf.dz3r.view.http.gae.v3.wire;

import com.fasterxml.jackson.annotation.JsonValue;

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

    /**
     * Force the Jackson 2.x serialization behavior.
     *
     * Unless this is done, HCC Remote fails to parse mode correctly.
     * This will become unnecessary when HCC Remote behavior is fixed.
     */
    @JsonValue
    public String jsonValue() {
        return name();
    }
}
