package net.sf.dz3.view.swing.thermostat;

import javax.swing.JPanel;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;

public abstract class AbstractChart extends JPanel implements DataSink<TintedValueAndSetpoint> {

    private static final long serialVersionUID = -6300058370712298736L;

}
