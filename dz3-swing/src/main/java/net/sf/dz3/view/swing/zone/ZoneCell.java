package net.sf.dz3.view.swing.zone;

import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneState;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityCell;
import net.sf.dz3.view.swing.EntitySelectorPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Condensed entity status indicator. {@link EntitySelectorPanel} displays these for all entities in a bar on top.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZoneCell extends EntityCell<ThermostatSignal> {

    private static final long serialVersionUID = 4736300051405383448L;

    public ZoneCell(Thermostat ts) {
        super(ts);

        setPreferredSize(new Dimension(20, 70));
        setToolTipText(ts.getName());
    }

    @Override
    protected void paintContent(Graphics2D g2d, Rectangle boundary) {
        paintGradient(
                getState(),
                getMode(),
                lastKnownSignal == null ? null : lastKnownSignal.sample.demand.sample,
                g2d, boundary);
    }

    @Override
    protected Color getBorderColor() {
        return ColorScheme.getScheme(getMode()).setpoint;
    }

    @Override
    protected Color getIndicatorColor() {

        var mode = getMode();

        switch (getState()) {

            case HAPPY:

                return ColorScheme.getScheme(mode).green;

            case ERROR:

                return ColorScheme.getScheme(mode).error;

            case OFF:

                return ColorScheme.getScheme(mode).off;

            default:

                return ColorScheme.getScheme(mode).bottom;
        }
    }

    private void paintGradient(ZoneState state, HvacMode mode, Double signal, Graphics2D g2d, Rectangle boundary) {

        switch (state) {

        case CALLING:
        case ERROR:
        case OFF:

    		BackgroundRenderer.drawBottom(state, mode, signal, g2d, boundary, false);
    		break;

        case HAPPY:

    		BackgroundRenderer.drawTop(mode, signal, g2d, boundary);
    		break;
        }
    }

    private HvacMode getMode() {
    	return ((AbstractPidController) ((ThermostatModel) source).getController()).getP() > 0 ? HvacMode.COOLING : HvacMode.HEATING;
    }

    private ZoneState getState() {

        if (lastKnownSignal == null || lastKnownSignal.isError() || lastKnownSignal.sample.demand.isError()) {
            return ZoneState.ERROR;
        }

        if (!lastKnownSignal.sample.enabled) {
            return ZoneState.OFF;
        }

        return lastKnownSignal.sample.calling ? ZoneState.CALLING : ZoneState.HAPPY;
    }
}
