package net.sf.dz3.view.swing.unit;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityPanel;
import net.sf.dz3.view.swing.ScreenDescriptor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class UnitPanel extends EntityPanel {

    private static final String UNDEFINED = "--";

    private final JLabel demandLabel = new JLabel("Demand", SwingConstants.LEFT);
    private final JLabel currentDemandLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final JLabel runningForLabel = new JLabel("Running for", SwingConstants.LEFT);
    private final JLabel currentRunningForLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final JLabel leftLabel = new JLabel("Time left", SwingConstants.LEFT);
    private final JLabel currentLeftLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final UnitChart unitChart = new UnitChart();

    private final transient RuntimePredictor source;

    /**
     * Create an instance.
     *
     * @param source Data source.
     */
    public UnitPanel(RuntimePredictor source, ScreenDescriptor screenDescriptor) {

        this.source = source;

        setFontSize(screenDescriptor);
        initGraphics();
        source.addConsumer(unitChart);
        source.addConsumer(new Listener());
    }

    @SuppressWarnings("squid:S1199")
    private void initGraphics() {
        createLayout(source.getName(), unitChart);
    }

    @Override
    @SuppressWarnings("squid:S1199")
    protected void createControls(JPanel controls, GridBagLayout layout, GridBagConstraints cs) {

        // VT: NOTE: squid:S1199 - SonarLint is not smart enough to realize that these
        // blocks are for readability

        // Demand
        {
            cs.gridwidth = 2;
            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.weightx = 1;
            cs.weighty = 0;

            layout.setConstraints(demandLabel, cs);
            controls.add(demandLabel);

            demandLabel.setForeground(ColorScheme.offMap.unitLabel);
        }
        {
            cs.gridx++;
            cs.gridwidth = 1;

            layout.setConstraints(currentDemandLabel, cs);
            controls.add(currentDemandLabel);

            currentDemandLabel.setForeground(ColorScheme.offMap.unitLabel);
        }

        // Running for
        {
            cs.gridx = 0;
            cs.gridy++;
            cs.gridwidth = 2;

            layout.setConstraints(runningForLabel, cs);
            controls.add(runningForLabel);

            runningForLabel.setForeground(ColorScheme.offMap.unitLabel);
        }
        {
            cs.gridx++;
            cs.gridwidth = 1;
            cs.weightx = 1;
            cs.weighty = 0;

            layout.setConstraints(currentRunningForLabel, cs);
            controls.add(currentRunningForLabel);

            currentRunningForLabel.setForeground(ColorScheme.offMap.unitLabel);
        }

        // ETA
        {
            cs.gridx = 0;
            cs.gridy++;
            cs.gridwidth = 2;

            layout.setConstraints(leftLabel, cs);
            controls.add(leftLabel);

            leftLabel.setForeground(ColorScheme.offMap.unitLabel);
        }
        {
            cs.gridx++;
            cs.gridwidth = 1;
            cs.weightx = 1;
            cs.weighty = 0;

            layout.setConstraints(currentLeftLabel, cs);
            controls.add(currentLeftLabel);

            currentLeftLabel.setForeground(ColorScheme.offMap.unitLabel);
        }
    }

    @Override
    public void setFontSize(ScreenDescriptor screenDescriptor) {

        demandLabel.setFont(screenDescriptor.fontSetpoint);
        currentDemandLabel.setFont(screenDescriptor.fontSetpoint);

        runningForLabel.setFont(screenDescriptor.fontSetpoint);
        currentRunningForLabel.setFont(screenDescriptor.fontSetpoint);

        leftLabel.setFont(screenDescriptor.fontSetpoint);
        currentLeftLabel.setFont(screenDescriptor.fontSetpoint);
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

    private class Listener implements DataSink<UnitRuntimePredictionSignal> {

        @Override
        public void consume(DataSample<UnitRuntimePredictionSignal> signal) {
            unitChart.consume(signal);
            displayDemand(signal);
            displayRunningFor(signal);
            displayLeft(signal);
        }

        private void displayDemand(DataSample<UnitRuntimePredictionSignal> signal) {

            var format = new DecimalFormat("#.###");
            format.setMinimumFractionDigits(1);

            if (signal.isError()) {
                currentDemandLabel.setText(UNDEFINED);
            } else {
                currentDemandLabel.setText(format.format(signal.sample.demand));
            }
        }

        private void displayRunningFor(DataSample<UnitRuntimePredictionSignal> signal) {
            if (signal.isError() || signal.sample.uptime == 0) {
                currentRunningForLabel.setText(UNDEFINED);
            } else {
                currentRunningForLabel.setText(format(Duration.of(signal.sample.uptime, ChronoUnit.MILLIS), false));
            }

            currentRunningForLabel.setForeground(getColor(Duration.of(signal.sample.uptime, ChronoUnit.MILLIS)));
        }

        private void displayLeft(DataSample<UnitRuntimePredictionSignal> signal) {
            if (signal.isError() || signal.sample.left == null) {
                currentLeftLabel.setText(UNDEFINED);
            } else {
                currentLeftLabel.setText(format(signal.sample.left, signal.sample.plus));
            }
            currentLeftLabel.setForeground(getColor(signal.sample.left));
        }

        private String format(Duration d, boolean plus) {

            if (d == null) {
                return UNDEFINED;
            }

            // Bottom out at 1 minute - it's weird to see zero here
            var seconds = Math.max(d.getSeconds(), 60);
            var hours = seconds / 3600;
            var minutes = (seconds % 3600) / 60;

            return (hours > 0 ? (hours + " hr ") : "")
                    + String.format("%2d min", minutes)
                    + (plus ? "+" : "");
        }

        private Color getColor(Duration d) {

            if (d == null) {
                return ColorScheme.offMap.unitLabel;
            }

            if (d.toMillis() == 0 ) {
                return ColorScheme.offMap.unitLabel;
            }

            if (d.compareTo(Duration.of(120, ChronoUnit.MINUTES)) > 0 ) {
                return ColorScheme.offMap.error.brighter();
            }

            if (d.compareTo(Duration.of(60, ChronoUnit.MINUTES)) > 0 ) {
                return ColorScheme.offMap.unitCritical;
            }

            if (d.compareTo(Duration.of(30, ChronoUnit.MINUTES)) > 0 ) {
                return ColorScheme.offMap.unitWarning;
            }

            return ColorScheme.offMap.unitNormal;
        }
    }
}
