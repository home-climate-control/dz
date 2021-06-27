package net.sf.dz3.view.swing;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public abstract class EntityCell<T> extends JPanel implements DataSink<T> {

    protected final transient Logger logger = LogManager.getLogger(getClass());
    private boolean selected = false;

    /**
     * How many pixels are there between the entity panel and entity cell bottom.
     */
    protected static final int PANEL_GAP = 4;

    /**
     * How many pixels are there between the indicator and zone cell.
     */
    protected static final int INDICATOR_GAP = 2;
    /**
     * How many pixels the zone status indicator takes.
     */
    protected static final int INDICATOR_HEIGHT = 10;

    protected final transient DataSource<T> source;
    protected DataSample<T> lastKnownSignal;

    protected EntityCell(DataSource<T> source) {
        this.source = source;
        source.addConsumer(this);
    }

    protected boolean isError() {
        return lastKnownSignal == null || lastKnownSignal.isError();
    }

    @Override
    public final void consume(DataSample<T> signal) {

        ThreadContext.push("consume@" + Integer.toHexString(hashCode()));

        try {

            this.lastKnownSignal = signal;
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
    public final void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    protected boolean isSelected() {
        return selected;
    }

    protected final void paintBorder(Color color, Graphics2D g2d, Rectangle boundary) {

        color = isSelected() ? color.brighter().brighter() : color.darker().darker();

        g2d.setColor(color);
        g2d.drawRect(1, 0, boundary.width - 2, boundary.height - 1);
    }
}
