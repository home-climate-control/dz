package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.device.model.UnitSignal;

/**
 * Runtime predictor producing a simple estimate with a minimum computing power consumption.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class NaiveRuntimePredictor implements RuntimePredictor {

    @Override
    public void consume(DataSample<UnitSignal> signal) {

    }
}
