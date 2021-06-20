package net.sf.dz3.device.model;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;

/**
 * An abstraction for predicting remaining unit runtime.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public interface RuntimePredictor extends DataSink<UnitSignal> {
}
