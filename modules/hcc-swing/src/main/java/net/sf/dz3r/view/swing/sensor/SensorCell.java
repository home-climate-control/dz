package net.sf.dz3r.view.swing.sensor;

import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityCell;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Optional;

public class SensorCell extends EntityCell<Double, Void> {

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
    protected boolean update(Signal<Double, Void> signal) {
        lastKnownSignal = Optional.ofNullable(signal).map(Signal::getValue).orElse(null);
        return true;
    }
}
