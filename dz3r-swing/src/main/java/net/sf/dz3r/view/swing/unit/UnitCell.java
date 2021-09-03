package net.sf.dz3r.view.swing.unit;

import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityCell;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class UnitCell extends EntityCell<HvacDeviceStatus, Void> {

    public UnitCell() {
        setPreferredSize(new Dimension(20, 70));
    }

    @Override
    protected void paintContent(Graphics2D g2d, Rectangle boundary) {

        if (getSignal() == null) {
            g2d.setPaint(ColorScheme.offMap.error);
            g2d.fill(boundary);
        }
    }

    @Override
    protected Color getBorderColor() {
        return ColorScheme.getScheme(getSignal() == null ? null : getSignal().getValue().requested.mode).setpoint;
    }

    @Override
    protected Color getIndicatorColor() {
        if (getSignal() == null) {
            return ColorScheme.offMap.error;
        }

        var mode = getSignal().getValue().requested.mode;
        return getSignal().getValue().requested.demand > 0 ? ColorScheme.getScheme(mode).setpoint : ColorScheme.getScheme(mode).setpoint.darker().darker();
    }

    @Override
    protected void consumeSignalValue(HvacDeviceStatus value) {
        // No special handling
    }
}
