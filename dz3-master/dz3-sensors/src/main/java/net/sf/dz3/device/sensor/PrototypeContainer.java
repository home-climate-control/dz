package net.sf.dz3.device.sensor;

/**
 * A device container that can prototype any device type requested from it.
 *
 * The address of the device to be produced is not necessarily the same as
 * the address of the prototype, but necessarily the address of one of its channels.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2000-2010
 */
public interface PrototypeContainer extends DeviceContainer {

    /**
     * Produce an analog sensor.
     *
     * @param address Address of the sensor to produce.
     * @param sensorType Type of the sensor to resolve.
     *
     * @return Analog sensor instance.
     */
    AnalogSensor getSensor(String address, SensorType sensorType);

    /**
     * Produce a switch.
     *
     * @param address Address of the switch to produce.
     *
     * @return Switch instance.
     */
    Switch getSwitch(String address);

    /**
     * @return Always {@link SensorType#PROTOTYPE}. This can't be enforced with the current
     * class tree, but must be observed.
     */
    @Override
    SensorType getType();
}
