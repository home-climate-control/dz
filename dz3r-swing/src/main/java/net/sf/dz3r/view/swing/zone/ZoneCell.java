package net.sf.dz3r.view.swing.zone;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.ZoneStatus;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityCell;
import reactor.core.publisher.Flux;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class ZoneCell extends EntityCell<ZoneStatus, Void> {

    private HvacMode hvacMode;

    public ZoneCell(String name) {
        setPreferredSize(new Dimension(20, 70));
        setToolTipText(name);
    }

    @Override
    protected void paintContent(Graphics2D g2d, Rectangle boundary) {

    }

    @Override
    protected Color getBorderColor() {
        return ColorScheme.getScheme(getMode()).setpoint;
    }

    @Override
    protected Color getIndicatorColor() {

        return Color.YELLOW;
    }

    private HvacMode getMode() {
        return hvacMode;
    }

    public void subscribeMode(Flux<Signal<HvacMode, Void>> hvacModeFlux) {
        hvacModeFlux.subscribe(this::getMode);
    }

    private void getMode(Signal<HvacMode, Void> hvacModeSignal) {
        this.hvacMode = hvacModeSignal.getValue();
        logger.info("hvacMode: {}", hvacMode);
        update();
    }
}
