package net.sf.dz3.view.swing.thermostat;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.ZoneState;
import net.sf.dz3.view.swing.ColorScheme;

public class BackgroundRenderer {

    /**
     * Draw the top gradient.
     *
     * @param sample Sample to get the drawing parameters from.
     * @param g2d Canvas to paint on.
     * @param boundary Drawing area boundary.
     */
    public static void drawTop(HvacMode mode, Double signal, Graphics2D g2d, Rectangle boundary) {

        double scale = signal;

        scale = scale > 2 ? 2 : scale;
        scale /= 2;

        int startHeight = (int)(boundary.height * scale);
        startHeight = startHeight > 0 ? startHeight : 1;

        Color startColor = getTopColor(mode);
        Color endColor = ColorScheme.getScheme(mode).background;

        GradientPaint gp = new GradientPaint(
                0, 0, startColor,
                0, startHeight, endColor);

        g2d.setPaint(gp);

        Rectangle2D.Double gradient = new Rectangle2D.Double(
                boundary.x, boundary.y,
                boundary.width, boundary.height);

        g2d.fill(gradient);
    }

    /**
     * Draw the bottom gradient.
     *
     * @param sample Sample to get the drawing parameters from.
     * @param g2d Canvas to paint on.
     * @param boundary Drawing area boundary.
     * @param errorGradient {@code true} if the gradient needs to be drawn, {@code false} if solid color.
     */
    public static void drawBottom(ZoneState state, HvacMode mode, Double signal, Graphics2D g2d, Rectangle boundary, boolean errorGradient) {

        double scale;

        if (state == null || state == ZoneState.ERROR) {

            if (!errorGradient) {

                Color bgColor = state == null
                        ? ColorScheme.offMap.error
                                : ColorScheme.getScheme(mode).error;

                g2d.setPaint(bgColor);

                Rectangle2D.Double background = new Rectangle2D.Double(boundary.x, boundary.y, boundary.width, boundary.height);

                g2d.fill(background);
                return;
            }

            scale = 0;

        } else if (state == ZoneState.OFF) {

        	scale = 0;

        } else {

            scale = signal;

            scale = scale > 2 ? 2 : scale;
            scale /= 2;
        }

        int startHeight = (int)(boundary.height * scale);
        startHeight = startHeight > 0 ? startHeight : 1;

        Color startColor = getBottomColor(state, mode);
        Color endColor = ColorScheme.getScheme(mode).background;

        GradientPaint gp = new GradientPaint(
                boundary.x, startHeight, endColor,
                boundary.x, boundary.height, startColor);

        g2d.setPaint(gp);

        Rectangle2D.Double gradient = new Rectangle2D.Double(
                boundary.x, boundary.y,
                boundary.width, boundary.height);

        g2d.fill(gradient);
    }

    /**
     * Get the color to {@link #drawTop(ZoneSnapshot, Canvas, int, int) paint the top gradient} with.
     *
     * @param mode HVAC mode to get the color from. {@code null} means error.
     *
     * @return Color to paint with.
     */
    private static Color getTopColor(HvacMode mode) {

        return mode == null ? ColorScheme.offMap.error : ColorScheme.getScheme(mode).top;
    }

    /**
     * Get the color to {@link #drawBottom(ZoneSnapshot, Graphics, int, int, boolean) paint the top gradient} with.
     *
     * @param state Zone state to get the color from. {@code null} means error.
     * @param mode HVAC mode to get the color from.
     *
     * @return Color to paint with.
     */
    private static Color getBottomColor(ZoneState state, HvacMode mode) {

        if (state == null || state == ZoneState.ERROR) {

            return ColorScheme.offMap.error;
        }

        if (state == ZoneState.OFF) {

            return ColorScheme.offMap.off;
        }

        if (mode == null) {

        	throw new IllegalStateException("mode == null");
        }

        return ColorScheme.getScheme(mode).bottom;
    }
}
