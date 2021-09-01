package net.sf.dz3r.view.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public abstract class EntityCell<T, P> extends SwingSink<T, P> {

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

    /**
     * Change the appearance depending on whether this cell is selected.
     *
     * @param selected {@code true} if this cell is selected.
     */
    public final void setSelected(boolean selected) {
        this.selected = selected;
        update();
    }

    protected boolean isSelected() {
        return selected;
    }

    @Override
    public final synchronized void paintComponent(Graphics g) {

        // Draw background
        super.paintComponent(g);

        var g2d = (Graphics2D) g;
        var d = getSize();

        // Background should be set, or edges will bleed white

        // VT: NOTE: See the note at ColorScheme#background
        g2d.setColor(ColorScheme.offMap.background);
        g2d.fillRect(0, 0, d.width, d.height);

        var statusBox = new Rectangle(1, 0, d.width - 2, d.height - PANEL_GAP - INDICATOR_HEIGHT - INDICATOR_GAP);
        var indicatorBox = new Rectangle(
                1, d.height - PANEL_GAP - INDICATOR_HEIGHT,
                d.width - 2, INDICATOR_HEIGHT);

        paintContent(g2d, statusBox);
        paintBorder(g2d, statusBox);
        paintIndicator(g2d, indicatorBox);
    }

    protected abstract void paintContent(Graphics2D g2d, Rectangle boundary);
    protected abstract Color getBorderColor();
    protected abstract Color getIndicatorColor();

    private void paintBorder(Graphics2D g2d, Rectangle boundary) {

        var color = getBorderColor();
        color = isSelected() ? color.brighter().brighter() : color.darker().darker();

        g2d.setColor(color);
        g2d.drawRect(1, 0, boundary.width - 2, boundary.height - 1);
    }

    private void paintIndicator(Graphics2D g2d, Rectangle boundary) {
        g2d.setPaint(getIndicatorColor());
        g2d.fillRect(boundary.x, boundary.y, boundary.width, boundary.height);
    }
}
