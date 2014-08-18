package net.sf.dz3.device.sensor;

import net.sf.jukebox.datastream.signal.model.DataSink;

/**
 * An analog filter.
 * 
 * For practical intents and purposes, may be used interchangeably with unfiltered {@link AnalogSensor}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2012
 */
public interface AnalogFilter extends AnalogSensor, DataSink<Double> {

}
