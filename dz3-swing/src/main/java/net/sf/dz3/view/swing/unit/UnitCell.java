package net.sf.dz3.view.swing.unit;

import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityCell;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class UnitCell extends EntityCell<UnitRuntimePredictionSignal> {

    public UnitCell(RuntimePredictor source) {
        super(source);
        setToolTipText(source.getName());
    }
    @Override
    public synchronized void paintComponent(Graphics g) {

        // Draw background
        super.paintComponent(g);

        var g2d = (Graphics2D) g;
        var d = getSize();

        // Background should be set, or edges will bleed white

        // VT: NOTE: See the note at ColorScheme#background
        g2d.setColor(ColorScheme.offMap.background);
        g2d.fillRect(0, 0, d.width, d.height);

        var statusBox = new Rectangle(1, 0, d.width - 2, d.height - PANEL_GAP - INDICATOR_HEIGHT - INDICATOR_GAP);
        var indicatorBox = new Rectangle(
                1, d.height - PANEL_GAP - INDICATOR_HEIGHT,
                d.width - 2, INDICATOR_HEIGHT);

        paintBorder(g2d, statusBox);
        paintIndicator(g2d, indicatorBox);
    }

    private void paintBorder(Graphics2D g2d, Rectangle boundary) {

        var mode = lastKnownSignal == null ? HvacMode.OFF : lastKnownSignal.sample.mode;
        super.paintBorder(ColorScheme.getScheme(mode).setpoint, g2d, boundary);
    }

    private void paintIndicator(Graphics2D g2d, Rectangle boundary) {

        Color bgColor;

        if (lastKnownSignal == null) {
            bgColor = ColorScheme.offMap.error;
        } else {
            var mode = lastKnownSignal == null ? HvacMode.OFF : lastKnownSignal.sample.mode;
            var running = lastKnownSignal != null && lastKnownSignal.sample.running;
            bgColor = running ? ColorScheme.getScheme(mode).setpoint : ColorScheme.getScheme(mode).green;
        }

        g2d.setPaint(bgColor);
        g2d.fillRect(boundary.x, boundary.y, boundary.width, boundary.height);
    }
}
