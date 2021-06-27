package net.sf.dz3.device.model;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;

/**
 * An abstraction for predicting remaining unit runtime.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public interface RuntimePredictor extends DataSink<HvacSignal>, DataSource<UnitRuntimePredictionSignal> {

    /**
     * Get the source name.
     *
     * Necessary evil to make things more manageable.
     *
     * @return Source name.
     */
    String getName();
}
