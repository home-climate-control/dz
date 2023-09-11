package net.sf.dz3r.view.swing.sensor;

import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityCell;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class SensorCell extends EntityCell<Double, Void> {

    /**
     * @see #consumeSignalValue(Double)
     */
    private transient Double lastKnownSignal = null;
    public SensorCell(String name) {
        setPreferredSize(new Dimension(20, 70));
        setToolTipText(name);
    }

    @Override
    protected void paintContent(Graphics2D g2d, Rectangle boundary) {

        if (lastKnownSignal == null) {
            g2d.setPaint(ColorScheme.offMap.error);
            g2d.fill(boundary);
        }
    }

    @Override
    protected Color getBorderColor() {
        return ColorScheme.offMap.sensorNormal;
    }

    @Override
    protected Color getIndicatorColor() {
        return isError() ? ColorScheme.offMap.sensorError.darker() : ColorScheme.offMap.sensorNormal.darker();
    }

    @Override
    protected void consumeSignalValue(Double value) {
        lastKnownSignal = value;
    }
}
