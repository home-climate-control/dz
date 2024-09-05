package net.sf.dz3r.view.swing.unit;

import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.HvacDeviceStatus;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityCell;
import reactor.core.publisher.Flux;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class UnitCell extends EntityCell<HvacDeviceStatus, Void> {

    public UnitCell(Flux<Signal<HvacDeviceStatus, Void>> signal) {
        setPreferredSize(new Dimension(20, 70));
        signal.subscribe(this::consumeSignal);
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
        return ColorScheme.getScheme(getSignal() == null ? null : getSignal().getValue().command().mode()).setpoint;
    }

    @Override
    protected Color getIndicatorColor() {
        if (getSignal() == null) {
            return ColorScheme.offMap.error;
        }

        var mode = getSignal().getValue().command().mode();
        return getSignal().getValue().command().demand() > 0 ? ColorScheme.getScheme(mode).setpoint : ColorScheme.getScheme(mode).setpoint.darker().darker();
    }

    @Override
    protected boolean update(Signal<HvacDeviceStatus, Void> signal) {
        // Caller has changed the state to make this meaningful
        return true;
    }
}
