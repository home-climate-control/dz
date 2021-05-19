package net.sf.dz3.device.sensor.impl.onewire;

import net.sf.dz3.device.sensor.HumiditySensor;
import net.sf.dz3.device.sensor.SensorType;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

import com.dalsemi.onewire.container.OneWireContainer;

/**
 * A platform independent humidity container.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2010
 */
public class OneWireHumidityContainer extends AbstractSensorContainer implements HumiditySensor {

    /**
     * Create an instance.
     *
     * @param container 1-Wire API container to base this container on.
     */
    OneWireHumidityContainer(final OneWireContainer container) {

        super(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final SensorType getType() {

        return SensorType.HUMIDITY;
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
              "1-Wire humidity sensor , adress " + getAddress());
    }
}