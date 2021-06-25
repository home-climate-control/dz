package net.sf.dz3.view.swing.sensor;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;

import javax.swing.JPanel;

public class SensorChart extends JPanel implements DataSink<Double> {
    @Override
    public void consume(DataSample<Double> signal) {

    }
}
