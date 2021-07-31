package net.sf.dz3.view.swing.sensor;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityPanel;
import net.sf.dz3.view.swing.ScreenDescriptor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.time.Clock;

public class SensorPanel extends EntityPanel {

    private static final String ERROR = "ERR";

    private final JLabel signalLabel = new JLabel("Signal", SwingConstants.LEFT);
    private final JLabel currentSignalLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final transient AnalogSensor source;
    private final SensorChart sensorChart = new SensorChart(Clock.systemUTC(), 1000L * 60 * 60 * 3);

    public SensorPanel(AnalogSensor source, ScreenDescriptor screenDescriptor) {

        this.source = source;

        setFontSize(screenDescriptor);
        initGraphics();
        source.addConsumer(sensorChart);
        source.addConsumer(new Listener());
    }

    private void initGraphics() {
        createLayout(source.getAddress(), sensorChart);
    }

    @Override
    @SuppressWarnings("squid:S1199")
    protected void createControls(JPanel controls, GridBagLayout layout, GridBagConstraints cs) {

        // VT: NOTE: squid:S1199 - SonarLint is not smart enough to realize that these
        // blocks are for readability

        // Signal
        {
            cs.gridwidth = 2;
            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.weightx = 1;
            cs.weighty = 0;

            layout.setConstraints(signalLabel, cs);
            controls.add(signalLabel);

            signalLabel.setForeground(ColorScheme.offMap.unitLabel);
        }
        {
            cs.gridx++;
            cs.gridwidth = 1;

            layout.setConstraints(currentSignalLabel, cs);
            controls.add(currentSignalLabel);

            currentSignalLabel.setForeground(ColorScheme.offMap.unitLabel);
        }
    }

    @Override
    public void setFontSize(ScreenDescriptor screenDescriptor) {
        signalLabel.setFont(screenDescriptor.fontSetpoint);
        currentSignalLabel.setFont(screenDescriptor.fontSetpoint);
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

    private class Listener implements DataSink<Double> {

        @Override
        public void consume(DataSample<Double> signal) {
            sensorChart.consume(signal);
            displaySignal(signal);
        }

        private void displaySignal(DataSample<Double> signal) {

            if (signal.isError()) {
                currentSignalLabel.setForeground(ColorScheme.offMap.error.brighter());
                currentSignalLabel.setText(ERROR);
                return;
            }

            var format = new DecimalFormat("#.###");
            format.setMinimumFractionDigits(1);

            currentSignalLabel.setForeground(ColorScheme.offMap.green);
            currentSignalLabel.setText(format.format(signal.sample));
        }
    }
}
