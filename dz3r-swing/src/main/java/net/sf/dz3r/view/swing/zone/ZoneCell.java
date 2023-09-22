package net.sf.dz3r.view.swing.zone;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityCell;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class ZoneCell extends EntityCell<ZoneStatus, Void> {

    /**
     * @see #consumeSignalValue(ZoneStatus)
     */
    private transient ZoneStatus zoneStatus;

    private HvacMode hvacMode;

    public ZoneCell(String name) {
        setPreferredSize(new Dimension(20, 70));
        setToolTipText(name);
    }

    @Override
    protected void paintContent(Graphics2D g2d, Rectangle boundary) {
        paintGradient(
                getState(),
                getMode(),
                zoneStatus == null ? null : zoneStatus.callingStatus.demand,
                g2d, boundary);
    }

    @Override
    protected Color getBorderColor() {
        return ColorScheme.getScheme(getMode()).setpoint;
    }

    @Override
    protected Color getIndicatorColor() {

        var mode = getMode();

        return switch (getState()) {
            case HAPPY -> ColorScheme.getScheme(mode).green;
            case ERROR -> ColorScheme.getScheme(mode).error;
            case OFF -> ColorScheme.getScheme(mode).off;
            default -> ColorScheme.getScheme(mode).bottom;
        };
    }

    private void paintGradient(Zone.State state, HvacMode mode, Double signal, Graphics2D g2d, Rectangle boundary) {

        switch (state) {
            case CALLING, ERROR, OFF -> BackgroundRenderer.drawBottom(state, mode, signal, g2d, boundary, false);
            case HAPPY -> BackgroundRenderer.drawTop(mode, signal, g2d, boundary);
        }
    }

    private HvacMode getMode() {
        return hvacMode;
    }

    public void consumeMode(Signal<HvacMode, Void> hvacModeSignal) {
        var hvacMode = hvacModeSignal.getValue(); // NOSONAR I know

        if (this.hvacMode == hvacMode) {
            return;
        }

        this.hvacMode = hvacMode;
        logger.debug("hvacMode: {}", hvacMode);
        update();
    }

    private Zone.State getState() {

        var signal = getSignal();
        if (signal == null || signal.isError() || zoneStatus == null) {
            return Zone.State.ERROR;
        }

        if (Boolean.FALSE.equals(zoneStatus.settings.enabled)) {
            return Zone.State.OFF;
        }

        return zoneStatus.callingStatus.calling ? Zone.State.CALLING : Zone.State.HAPPY;
    }

    @Override
    protected void consumeSignalValue(ZoneStatus zoneStatus) {

        if (zoneStatus == null) {
            logger.warn("null zoneStatus update, ignored");
            return;
        }

        this.zoneStatus = zoneStatus;
    }
}
