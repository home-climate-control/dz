package net.sf.dz3.view.swing.unit;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;

import javax.swing.JPanel;

public class UnitChart extends JPanel implements DataSink<UnitRuntimePredictionSignal> {


    @Override
    public void consume(DataSample<UnitRuntimePredictionSignal> signal) {

    }
}
