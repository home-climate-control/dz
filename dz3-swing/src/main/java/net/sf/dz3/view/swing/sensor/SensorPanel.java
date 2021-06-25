package net.sf.dz3.view.swing.sensor;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.swing.ScreenDescriptor;
import net.sf.dz3.view.swing.thermostat.EntityPanel;

import javax.swing.JPanel;
import java.awt.event.KeyEvent;

public class SensorPanel extends EntityPanel {

    private final SensorChart sensorChart = new SensorChart();

    public SensorPanel(AnalogSensor source) {
        source.addConsumer(sensorChart);
    }

    @Override
    protected JPanel createControls() {
        return null;
    }

    @Override
    public void setFontSize(ScreenDescriptor screenDescriptor) {
        // No special handling yet
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No special handling
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // No special handling yet
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No special handling
    }
}
