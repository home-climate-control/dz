package net.sf.dz3r.device.actuator;

import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.HvacDeviceStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Common functionality for all HVAC device drivers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
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

    public abstract static class AbstractHvacDeviceStatus extends HvacDeviceStatus {

        public enum Kind {
            REQUESTED,
            ACTUAL
        }

        public final Kind kind;
        public final HvacCommand requested;

        protected AbstractHvacDeviceStatus(Kind kind, HvacCommand requested) {
            this.kind = kind;
            this.requested = requested;
        }

        @Override
        public String toString() {
            return "{kind=" + kind + ", requested=" + requested + "}";
        }
    }
}
