package net.sf.dz3.view.swing.zone;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneState;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityCell;
import net.sf.dz3.view.swing.EntitySelectorPanel;
import org.apache.logging.log4j.ThreadContext;

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

    /**
     * How many pixels are there between the zone panel and zone cell bottom.
     */
    private static final int ZONE_PANEL_GAP = 4;

    /**
     * How many pixels are there between the indicator and zone cell.
     */
    private static final int INDICATOR_GAP = 2;
    /**
     * How many pixels the zone status indicator takes.
     */
    private static final int INDICATOR_HEIGHT = 10;

    private boolean selected = false;

    private DataSample<ThermostatSignal> signal;
    private final Thermostat source;

    public ZoneCell(Thermostat ts) {

        this.source = ts;
        ts.addConsumer(this);

        setPreferredSize(new Dimension(20, 70));
        setToolTipText(ts.getName());
    }

    @Override
    public void consume(DataSample<ThermostatSignal> signal) {

        ThreadContext.push("consume@" + Integer.toHexString(hashCode()));

        try {

            this.signal = signal;
            logger.trace(signal);
            repaint();

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Change the appearance depending on whether this cell is selected.
     *
     * @param selected {@code true} if this cell is selected.
     */
    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
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

        var signal = this.signal == null ? null : this.signal.sample.demand.sample;
        var mode = getMode();
        var state = this.signal == null ? ZoneState.ERROR : (this.signal.sample.calling ? ZoneState.CALLING : ZoneState.HAPPY); // NOSONAR Simple enough

        if ( this.signal == null || this.signal.sample.demand.isError()) {

        	state = ZoneState.ERROR;

        } else if (!this.signal.sample.enabled) {

        	state = ZoneState.OFF;
        }

        var upper = new Rectangle(1, 0, d.width - 2, d.height - ZONE_PANEL_GAP - INDICATOR_HEIGHT - INDICATOR_GAP);
        var indicator = new Rectangle(
        		1, d.height - ZONE_PANEL_GAP - INDICATOR_HEIGHT,
        		d.width - 2, INDICATOR_HEIGHT);

        paintGradient(state, mode, signal, g2d, upper);
        paintBorder(mode, g2d, upper);
        paintIndicator(state, mode, g2d, indicator);
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

        var borderColor = ColorScheme.getScheme(mode).setpoint;

        borderColor = selected ? borderColor.brighter().brighter() : borderColor.darker().darker();

        g2d.setColor(borderColor);
        g2d.drawRect(1, 0, boundary.width - 2, boundary.height - 1);
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
