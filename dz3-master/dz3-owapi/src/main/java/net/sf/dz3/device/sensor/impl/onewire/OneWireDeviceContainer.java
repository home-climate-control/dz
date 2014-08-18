package net.sf.dz3.device.sensor.impl.onewire;

import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.impl.AbstractDeviceContainer;

import com.dalsemi.onewire.container.OneWireContainer;

public class OneWireDeviceContainer extends AbstractDeviceContainer {

    /**
     * 1-Wire API device container.
     */
    public final OneWireContainer container;

    /**
     * Create an instance.
     * 
     * @param container
     *            1-Wire API device container to base this container on.
     */
    public OneWireDeviceContainer(final OneWireContainer container) {

        if (container == null) {

            throw new IllegalArgumentException("container can't be null");
        }

        this.container = container;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getName() {

        return container.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getAddress() {

        return container.getAddressAsString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SensorType getType() {

        return SensorType.GENERIC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {

        return container.toString();
    }
}
