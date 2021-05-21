package net.sf.dz3.device.sensor.impl;

import net.sf.dz3.device.sensor.DeviceContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation independent device container abstraction.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2010
 */
abstract public class AbstractDeviceContainer implements DeviceContainer {

    protected final Logger logger = LogManager.getLogger(getClass());

    @Override
    public final String getSignature() {
        return getType() + getAddress();
    }

    @Override
    public int compareTo(DeviceContainer other) {

        if (other == null) {
            throw new IllegalArgumentException("Can't compare to null");
        }

        return getSignature().compareTo(((DeviceContainer) other).getSignature());
    }
}
