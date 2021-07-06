package net.sf.dz3.view.swing.sensor;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityCell;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class SensorCell extends EntityCell<Double> {

    public SensorCell(AnalogSensor source) {
        super(source);
        setToolTipText(source.getAddress());
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
        return isError() ? ColorScheme.offMap.sensorError.darker() : ColorScheme.offMap.sensorNormal.brighter();
    }
}