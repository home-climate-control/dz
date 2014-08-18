package net.sf.dz3.device.sensor.impl.onewire;

import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.TemperatureSensor;
import net.sf.jukebox.jmx.JmxDescriptor;

import com.dalsemi.onewire.container.OneWireContainer;

/**
 * A platform independent temperature container.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public class OneWireTemperatureContainer extends AbstractSensorContainer implements TemperatureSensor {

    /**
     * Create an instance.
     * 
     * @param container 1-Wire API container to base this container on.
     */
    OneWireTemperatureContainer(final OneWireContainer container) {

        super(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final SensorType getType() {

        return SensorType.TEMPERATURE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmxDescriptor getJmxDescriptor() {
        
      return new JmxDescriptor(
              "dz",
              getClass().getSimpleName(),
              Integer.toHexString(hashCode()),
              "1-Wire temperature sensor , adress " + getAddress());
    }
}
