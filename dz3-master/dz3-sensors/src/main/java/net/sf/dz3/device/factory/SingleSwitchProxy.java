package net.sf.dz3.device.factory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.sensor.DeviceContainer;
import net.sf.dz3.device.sensor.PrototypeContainer;
import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.ContainerMap;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
 * A proxy for a single channel switch device.
 *
 * @param <T> Implementation class of the hardware dependent switch container.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com"> Vadim Tkachenko 2001-2020
 */
public abstract class SingleSwitchProxy<T> implements Switch {

    protected final Logger logger = LogManager.getLogger(getClass());

    protected final ContainerMap address2dcGlobal;
    protected final StringChannelAddress address;

    public SingleSwitchProxy(ContainerMap address2dcGlobal, StringChannelAddress address) {

        this.address2dcGlobal = address2dcGlobal;
        this.address = address;
    }

    /**
     * Get the device container.
     *
     * @param address Hardware address.
     *
     * @return Device container.
     *
     * @exception IOException if the device container is not present.
     */
    @SuppressWarnings("unchecked")
    protected final T getContainer(String address) throws IOException {

        ThreadContext.push("getContainer");

        try {

            Set<DeviceContainer> devices = address2dcGlobal.get(address);

            if (devices == null) {

                throw new IOException("No container found for " + address + ", assuming not present");
            }

            for (Iterator<DeviceContainer> i = devices.iterator(); i.hasNext(); ) {

                DeviceContainer dc = i.next();
                logger.debug("Found: " + dc + ", " + dc.getType());

                if (SensorType.SWITCH.equals(dc.getType())) {

                    // Voila, we already have it
                    return (T) dc;
                }

                if (dc.getType().equals(SensorType.PROTOTYPE)) {

                    return (T) ((PrototypeContainer) dc).getSwitch(address);
                }
            }

            throw new IOException("Device present at " + address + ", not a switch nor a prototype");

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public final String getAddress() {
        return address.toString();
    }

    @Override
    public final void addConsumer(DataSink<Boolean> consumer) {
        throw new IllegalStateException("this object should never be watched");
    }

    @Override
    public final void removeConsumer(DataSink<Boolean> consumer) {
        throw new IllegalStateException("this object should never be watched");
    }
}
