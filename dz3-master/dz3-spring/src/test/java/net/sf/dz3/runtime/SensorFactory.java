package net.sf.dz3.runtime;

import net.sf.dz3.device.sensor.AnalogSensor;


public interface SensorFactory<Descriptor extends SensorDescriptor> {

    AnalogSensor getSensor(Descriptor descriptor);
}
