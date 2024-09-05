package com.homeclimatecontrol.hcc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * HVAC device mode.
 *
 * Unlike the previous incarnation, this just includes actionable modes, {@code OFF} is handled by devices.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public enum HvacMode {

    @JsonProperty("cooling")
    COOLING(-1, "Cooling"),
    @JsonProperty("heating")
    HEATING(1, "Heating");

    public final int mode;
    public final String description;

    private HvacMode(int mode, String description) {
        this.mode = mode;
        this.description = description;
    }
}
