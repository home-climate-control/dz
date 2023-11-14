package com.homeclimatecontrol.hcc.meta;

/**
 * Data set sufficient to initialize a simple HVAC device UI element.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public record HvacDeviceMeta(
        String id,
        Type type,
        boolean hasFilter
) {

    /**
     * Values are the leaf nodes of the {@code HvacDevice} hierarchy.
     */
    enum Type {
        HEAT_PUMP,
        SWITCHABLE,
        SWITCHABLE_STACKED,
        VARIABLE
    }
}
