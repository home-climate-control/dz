package net.sf.dz3r.device.actuator;

import net.sf.dz3.device.sensor.Switch;

public abstract class AbstractHvacDevice implements HvacDevice {

    private final String name;

    protected AbstractHvacDevice(String name) {
        this.name = name;
    }

    @Override
    public final String getAddress() {
        return name;
    }

    protected void check(Switch s, String purpose) {

        if (s == null) {
            throw new IllegalArgumentException("'" + purpose + "' switch can't be null");
        }
    }

}
