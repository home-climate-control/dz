package net.sf.dz3.view.swing.sensor;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityCell;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class SensorCell extends EntityCell<Double> {

    public SensorCell(AnalogSensor source) {
        super(source);
        setToolTipText(source.getAddress());
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

        paintBorder(ColorScheme.offMap.sensorNormal, g2d, statusBox);
        paintIndicator(g2d, indicatorBox);
    }

    private void paintIndicator(Graphics2D g2d, Rectangle boundary) {
        g2d.setPaint(isError() ? ColorScheme.offMap.sensorError.darker() : ColorScheme.offMap.sensorNormal.brighter());
        g2d.fillRect(boundary.x, boundary.y, boundary.width, boundary.height);
    }
}
