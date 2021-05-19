package net.sf.dz3.device.sensor;

/**
 * Factory for sensors and actuators, 2020 edition.
 *
 * The difference from original {@link DeviceFactory} is that it is now
 * recognized that providers must not care about what kind of sensors they
 * create, and it's the consumers who are responsible for correctly interpreting
 * the data they will receive.
 *
 * This class' documentation will also skip the drivel about how the returned
 * objects must behave, deferring it to actual implementation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public interface DeviceFactory2020 {

    /**
     * Get a sensor instance.
     *
     * @param address Hardware address.
     *
     * @return A sensor instance.
     */
    AnalogSensor getSensor(String address);

    /**
     * Get a switch instance.
     *
     * @param address Hardware address.
     *
     * @return A {@link Switch single channel switch} switch instance.
     */
    Switch getSwitch(String address);
}
