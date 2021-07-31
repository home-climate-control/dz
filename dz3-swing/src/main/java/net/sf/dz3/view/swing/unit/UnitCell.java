package net.sf.dz3.view.swing.unit;

import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityCell;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class UnitCell extends EntityCell<UnitRuntimePredictionSignal> {

    public UnitCell(RuntimePredictor source) {
        super(source);
        setToolTipText(source.getName());
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
        return ColorScheme.getScheme(lastKnownSignal == null ? HvacMode.OFF : lastKnownSignal.sample.mode).setpoint;
    }

    @Override
    protected Color getIndicatorColor() {

        if (lastKnownSignal == null) {
            return ColorScheme.offMap.error;
        }

        var mode = lastKnownSignal.sample.mode;
        return lastKnownSignal.sample.running ? ColorScheme.getScheme(mode).setpoint : ColorScheme.getScheme(mode).setpoint.darker().darker();
    }
}
