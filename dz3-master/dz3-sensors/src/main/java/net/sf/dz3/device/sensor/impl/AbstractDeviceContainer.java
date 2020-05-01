package net.sf.dz3.device.sensor.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.sf.dz3.device.sensor.DeviceContainer;
import net.sf.dz3.device.sensor.SensorType;

/**
 * Implementation independent device container abstraction.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2000-2010
 */
abstract public class AbstractDeviceContainer implements DeviceContainer {

    protected final Logger logger = LogManager.getLogger(getClass());

    /**
     * @return Device name.
     */
    @Override
    abstract public String getName();

    /**
     * @return Device address.
     */
    @Override
    abstract public String getAddress();

    /**
     * @return Device type.
     */
    @Override
    abstract public SensorType getType();

    /**
     * @return Device signature.
     */
    @Override
    public final String getSignature() {

        return getType() + getAddress();
    }

    @Override
    public int compareTo(DeviceContainer other) {

        if (other == null) {

            throw new IllegalArgumentException("Can't compare to null");
        }

        return getSignature().compareTo(other.getSignature());
    }
}
