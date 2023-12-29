package net.sf.dz3r.view.swing.unit;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityPanel;
import net.sf.dz3r.view.swing.ScreenDescriptor;
import reactor.core.publisher.Flux;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class UnitPanel extends EntityPanel<HvacDeviceStatus, Void> {

    private final String name;

    private final JLabel demandLabel = new JLabel("Demand", SwingConstants.LEFT);
    private final JLabel currentDemandLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final JLabel runningForLabel = new JLabel("Running for", SwingConstants.LEFT);
    private final JLabel currentRunningForLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final JLabel leftLabel = new JLabel("Time left", SwingConstants.LEFT);
    private final JLabel currentLeftLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final UnitChart unitChart = new UnitChart(Clock.systemUTC());

    public UnitPanel(String name, ScreenDescriptor screenDescriptor, Flux<Signal<HvacDeviceStatus, Void>> signal) {

        this.name = name;

        setFontSize(screenDescriptor);
        initGraphics();

        signal.subscribe(this::consumeSignal);
    }

    @SuppressWarnings("squid:S1199")
    private void initGraphics() {
        createLayout(name, unitChart);
    }

    @Override
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
    protected boolean update(Signal<HvacDeviceStatus, Void> signal) {

        displayDemand(signal);
        displayRunningFor(signal);
        displayLeft(signal);

        return true;
    }
    private void displayDemand(Signal<HvacDeviceStatus, Void> signal) {

        var format = new DecimalFormat("#.###");
        format.setMinimumFractionDigits(1);

        if (signal.isError()) {
            currentDemandLabel.setText(UNDEFINED);
        } else {
            currentDemandLabel.setText(format.format(signal.getValue().command.demand));
        }
    }

    private void displayRunningFor(Signal<HvacDeviceStatus, Void> signal) {
        if (signal.isError() || signal.getValue().uptime == null) {
            currentRunningForLabel.setText(UNDEFINED);
        } else {
            currentRunningForLabel.setText(format(signal.getValue().uptime, false));
        }

        currentRunningForLabel.setForeground(getColor(signal.getValue().uptime));
    }

    private void displayLeft(Signal<HvacDeviceStatus, Void> signal) {

        // VT: FIXME: Predictor is not ported yet

//        if (signal.isError() || signal.sample.left == null) {
//            currentLeftLabel.setText(UNDEFINED);
//        } else {
//            currentLeftLabel.setText(format(signal.sample.left, signal.sample.plus));
//        }
//        currentLeftLabel.setForeground(getColor(signal.sample.left));
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

    static String format(Duration d, boolean plus) {

        if (d == null) {
            return UNDEFINED;
        }

        var seconds = d.getSeconds();
        var hours = seconds / 3600;
        var minutes = (seconds % 3600) / 60;

        return (hours > 0 ? (hours + " hr") : "") + renderMinutes(hours, minutes) + (plus ? "+" : "");
    }

    private static String renderMinutes(long hours, long minutes) {

        // Zero hours, zero minutes
        if (hours == 0 && minutes == 0) {
            return "<1 min";
        }

        // X hours exactly
        if (hours > 0 && minutes == 0) {
            return "";
        }

        return String.format("%s%2d min", hours > 0 ? " " : "", minutes);
    }
}
