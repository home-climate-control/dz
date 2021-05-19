package net.sf.dz3.device.sensor;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;

/**
 * 1-Wire device that is a single channel signal sensor.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2012
 */
public interface SensorDeviceContainer<E> extends DeviceContainer, DataSource<Double> {

}
