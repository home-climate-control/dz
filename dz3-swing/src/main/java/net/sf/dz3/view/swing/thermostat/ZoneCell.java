package net.sf.dz3.view.swing.thermostat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneState;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.view.swing.ColorScheme;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;

/**
 * Condensed zone status indicator. {@link ZonePanel} displays these for all zones in a bar on top.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class ZoneCell extends JPanel implements DataSink<ThermostatSignal> {

    private static final long serialVersionUID = 4736300051405383448L;

    private final Logger logger = LogManager.getLogger(getClass());

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
    public void setSelected(boolean selected) {

        this.selected = selected;

        repaint();
    }

    @Override
    public synchronized void paintComponent(Graphics g) {

        // Draw background
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        Dimension d = getSize();

        // Background should be set, or edges will bleed white

        g2d.setColor(ColorScheme.getScheme(getMode()).background);
        g2d.fillRect(0, 0, d.width, d.height);

        Double signal = this.signal == null ? null : this.signal.sample.demand.sample;
        HvacMode mode = getMode();
        ZoneState state = this.signal == null ? ZoneState.ERROR : (this.signal.sample.calling ? ZoneState.CALLING : ZoneState.HAPPY);

        if ( this.signal == null || this.signal.sample.demand.isError()) {

        	state = ZoneState.ERROR;

        } else if (!this.signal.sample.enabled) {

        	state = ZoneState.OFF;
        }

        Rectangle upper = new Rectangle(1, 0, d.width - 2, d.height - ZONE_PANEL_GAP - INDICATOR_HEIGHT - INDICATOR_GAP);
        Rectangle indicator = new Rectangle(
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

        Color borderColor = ColorScheme.getScheme(mode).setpoint;

        if (selected) {

        	borderColor = borderColor.brighter().brighter();

        } else {

        	borderColor = borderColor.darker().darker();
        }

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
