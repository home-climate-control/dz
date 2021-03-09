package net.sf.dz3.device.sensor;

import java.io.IOException;

import net.sf.jukebox.datastream.signal.model.DataSample;

/**
 * Factory for sensors and actuators.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2020
 */
public interface DeviceFactory {

    public enum Type {
        TEMPERATURE,
        HUMIDITY,
        SWITCH
    }

    /**
     * Get an instance of a temperature sensor.
     * 
     * @param address Hardware address.
     * 
     * @return An instance of a temperature sensor, unconditionally. In case when
     * the device with a given address is not present, the instance returned will keep
     * producing {@link DataSample error samples} over and over, with "Not Present" being the error. 
     */
    AnalogSensor getTemperatureSensor(String address);

    /**
     * Get an instance of a humidity sensor.
     * 
     * @param address Hardware address.
     * 
     * @return An instance of a humidity sensor, unconditionally. In case when
     * the device with a given address is not present, the instance returned will keep
     * producing {@link DataSample error samples} over and over, with "Not Present" being the error. 
     */
    AnalogSensor getHumiditySensor(String address);

    /**
     * Get an instance of a switch.
     * 
     * @param address Hardware address.
     * 
     * @return An instance of a {@link Switch single channel switch}, unconditionally. In case when
     * the device with a given address is not present on a bus, no indication will be given
     * until the actual operation (accessor or mutator) is performed, which will result in
     * {@link IOException}.
     */
    Switch getSwitch(String address);
}