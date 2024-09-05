package net.sf.dz3r.view.swing.sensor;


import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityPanel;
import net.sf.dz3r.view.swing.ScreenDescriptor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.time.Clock;

public class SensorPanel extends EntityPanel<Double, Void> {

    private static final String ERROR = "ERR";

    private final JLabel signalLabel = new JLabel("Signal", SwingConstants.LEFT);
    private final JLabel currentSignalLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final SensorChart sensorChart = new SensorChart(Clock.systemUTC(), 1000L * 60 * 60 * 3);

    public SensorPanel(String name, ScreenDescriptor screenDescriptor) {

        setToolTipText(name);
        setFontSize(screenDescriptor);
        initGraphics(name);
    }

    private void initGraphics(String name) {
        createLayout(name, sensorChart);
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
    protected boolean update(Signal<Double, Void> signal) {

        if (signal.isError()) {
            currentSignalLabel.setForeground(ColorScheme.offMap.error.brighter());
            currentSignalLabel.setText(ERROR);
            return true;
        }

        var format = new DecimalFormat("#.###");
        format.setMinimumFractionDigits(1);

        currentSignalLabel.setForeground(ColorScheme.offMap.green);
        currentSignalLabel.setText(format.format(signal.getValue()));

        sensorChart.consumeSignal(signal);

        return true;
    }
}
