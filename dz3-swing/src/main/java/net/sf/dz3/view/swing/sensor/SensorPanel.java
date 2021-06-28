package net.sf.dz3.view.swing.sensor;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.swing.EntityPanel;
import net.sf.dz3.view.swing.ScreenDescriptor;

import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;

public class SensorPanel extends EntityPanel {

    private final transient AnalogSensor source;
    private final SensorChart sensorChart = new SensorChart();

    public SensorPanel(AnalogSensor source, ScreenDescriptor screenDescriptor) {

        this.source = source;

        setFontSize(screenDescriptor);
        initGraphics();
        source.addConsumer(sensorChart);
    }

    private void initGraphics() {
        createLayout(source.getAddress(), sensorChart);
    }

    @Override
    protected void createControls(JPanel controls, GridBagLayout layout, GridBagConstraints cs) {
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
