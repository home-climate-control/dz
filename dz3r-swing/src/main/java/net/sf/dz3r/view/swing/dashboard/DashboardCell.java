package net.sf.dz3r.view.swing.dashboard;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.health.SystemStatus;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityCell;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class DashboardCell extends EntityCell<SystemStatus, Void> {

    public DashboardCell() {
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
        // Mode has no say in this case, unlike every other cell
        return ColorScheme.offMap.green;
    }

    @Override
    protected Color getIndicatorColor() {

        if (getSignal() == null || getSignal().isError()) {
            return ColorScheme.offMap.error;
        }

        return ColorScheme.offMap.green.darker().darker();
    }

    @Override
    protected boolean update(Signal<SystemStatus, Void> signal) {
        // Caller has changed the state to make this meaningful
        return true;
    }
}
