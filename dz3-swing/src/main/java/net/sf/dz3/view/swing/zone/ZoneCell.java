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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Condensed entity status indicator. {@link EntitySelectorPanel} displays these for all entities in a bar on top.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZoneCell extends EntityCell<ThermostatSignal> {

    private static final long serialVersionUID = 4736300051405383448L;

    /**
     * How many pixels are there between zone cells.
     */
    private static final int HORIZONTAL_PADDING = 1;

    public ZoneCell(Thermostat ts) {
        super(ts);

        setPreferredSize(new Dimension(20, 70));
        setToolTipText(ts.getName());
    }

    @Override
    public synchronized void paintComponent(Graphics g) {

        // Draw background
        super.paintComponent(g);

        var g2d = (Graphics2D) g;
        var d = getSize();

        // Background should be set, or edges will bleed white

        g2d.setColor(ColorScheme.getScheme(getMode()).background);
        g2d.fillRect(0, 0, d.width, d.height);

        var signal = this.lastKnownSignal == null ? null : this.lastKnownSignal.sample.demand.sample;
        var mode = getMode();
        var state = this.lastKnownSignal == null ? ZoneState.ERROR : (this.lastKnownSignal.sample.calling ? ZoneState.CALLING : ZoneState.HAPPY); // NOSONAR Simple enough

        if ( this.lastKnownSignal == null || this.lastKnownSignal.sample.demand.isError()) {
        	state = ZoneState.ERROR;
        } else if (!this.lastKnownSignal.sample.enabled) {
        	state = ZoneState.OFF;
        }

        var statusBox = new Rectangle(1, 0, d.width - 2, d.height - PANEL_GAP - INDICATOR_HEIGHT - INDICATOR_GAP);
        var indicatorBox = new Rectangle(
                1, d.height - PANEL_GAP - INDICATOR_HEIGHT,
                d.width - 2, INDICATOR_HEIGHT);

        paintGradient(state, mode, signal, g2d, statusBox);
        paintBorder(mode, g2d, statusBox);
        paintIndicator(state, mode, g2d, indicatorBox);
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

    private void paintBorder(HvacMode mode, Graphics2D g2d, Rectangle boundary) {
        super.paintBorder(ColorScheme.getScheme(mode).setpoint, g2d, boundary);
    }

    private void paintIndicator(ZoneState state, HvacMode mode, Graphics2D g2d, Rectangle boundary) {

        Color bgColor;

        switch (state) {

            case HAPPY:

                bgColor = ColorScheme.getScheme(mode).green;
                break;

            case ERROR:

                bgColor = ColorScheme.getScheme(mode).error;
                break;

            case OFF:

                bgColor = ColorScheme.getScheme(mode).off;
                break;

            default:

                bgColor = ColorScheme.getScheme(mode).bottom;
        }

        g2d.setPaint(bgColor);
        g2d.fillRect(boundary.x, boundary.y, boundary.width, boundary.height);
    }

    private HvacMode getMode() {
    	return ((AbstractPidController) ((ThermostatModel) source).getController()).getP() > 0 ? HvacMode.COOLING : HvacMode.HEATING;
    }
}
