package net.sf.dz3r.device.actuator;

import net.sf.dz3.device.sensor.Switch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractHvacDevice implements HvacDevice {

    protected final Logger logger = LogManager.getLogger();

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
