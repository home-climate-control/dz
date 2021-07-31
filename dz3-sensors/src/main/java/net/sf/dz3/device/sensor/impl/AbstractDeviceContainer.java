package net.sf.dz3.device.sensor.impl;

import net.sf.dz3.device.sensor.Addressable;
import net.sf.dz3.device.sensor.DeviceContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation independent device container abstraction.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
abstract public class AbstractDeviceContainer implements DeviceContainer {

    protected final Logger logger = LogManager.getLogger(getClass());

    @Override
    public final String getSignature() {
        return getType() + getAddress();
    }

    @Override
    public int compareTo(Addressable o) {
        // Can't afford to collide with the wrapper
        return (getClass().getName() + getAddress()).compareTo((o.getClass().getName() + o.getAddress()));
    }
}
