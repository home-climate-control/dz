package net.sf.dz3r.view.swing.zone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Color;

public class SignalColorCache {

    private final Logger logger = LogManager.getLogger();

    /**
     * Color corresponding to {@code -1} signal value.
     */
    private final Color low;

    /**
     * Color corresponding to {@code 1} signal value.
     */
    private final Color high;
    private final Color[] signalCache = new Color[256];

    /**
     * Cache medium for {@link #resolve(Color)}.
     *
     * According to "worse is better" rule, there's no error checking against
     * the array size - too expensive. In all likelihood, this won't grow beyond 2 entries.
     */
    private final RGB2HSB[] rgb2hsb = new RGB2HSB[16];

    public SignalColorCache(Color low, Color high) {
        this.low = low;
        this.high = high;
    }

    /**
     * Convert signal from -1 to +1 to color from low color to high color.
     *
     * @param signal Signal to convert to color.
     *
     * @return Color resolved from the incoming signal.
     */
    public Color signal2color(double signal) {
        return signal2color(signal, 0xFF);
    }

    /**
     * Convert signal from -1 to +1 to color from low color to high color with a given alpha channel value.
     *
     * @param signal Signal to convert to color.
     * @param alpha Alpha channel value to apply.
     *
     * @return Color resolved from the incoming signal and alpha channel.
     */
    public Color signal2color(double signal, int alpha) {

        var limited = signal > 1 ? 1: signal;
        limited = limited < -1 ? -1 : limited;
        var centered = (limited + 1) / 2;

        int index = (int) (centered * 255);

        synchronized (signalCache) {

            Color result = signalCache[index];

            if ( result == null) {

                float[] hsbLow = resolve(low);
                float[] hsbHigh = resolve(high);

                float h = transform(centered, hsbLow[0], hsbHigh[0]);
                float s = transform(centered, hsbLow[1], hsbHigh[1]);
                float b = transform(centered, hsbLow[2], hsbHigh[2]);

                result = new Color(((alpha & 0xFF) << 24) | Color.HSBtoRGB(h, s, b));
                signalCache[index] = result;

                logger.debug("signal2color({}, {}): {} -> {}",
                        Integer.toHexString(low.getRGB()),
                        Integer.toHexString(high.getRGB()),
                        signal, Integer.toHexString(result.getRGB()));
            }

            return result;
        }
    }

    /**
     * Resolve a possibly cached {@link Color#RGBtoHSB(int, int, int, float[])} result,
     * or compute it and store it for later retrieval if it hasn't been done.
     *
     * @param color Color to transform.
     * @return Transformation result.
     */
    private float[] resolve(Color color) {

        var rgb = color.getRGB();
        var offset = 0;

        for (; offset < rgb2hsb.length && rgb2hsb[offset] != null; offset++) {

            if (rgb == rgb2hsb[offset].rgb) {
                return rgb2hsb[offset].hsb;
            }
        }

        synchronized (rgb2hsb) {

            // VT: NOTE: Not the cleanest solution. It is possible that someone has just tried to do the same thing
            // and we'll end up writing the same value twice, but oh well, it's the same value in an array of size 16

            rgb2hsb[offset] = new RGB2HSB(rgb, Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null));
        }

        logger.info("RGB2HSB offset={}", offset );

        return rgb2hsb[offset].hsb;
    }

    /**
     * Get the point between the start and end values corresponding to the value of the signal.
     *
     * @param signal Signal value, from -1 to +1.
     * @param start Start point.
     * @param end End point.
     *
     * @return Desired position between the start and end points.
     */
    private float transform(double signal, float start, float end) {

        if (signal < -1.0 || signal > 1.0) {
            throw new IllegalArgumentException("signal (" + signal + ") is outside of -1...1 range ");
        }

        return (float) (start + signal * (end - start));
    }

    private static class RGB2HSB {

        public final int rgb;
        public final float[] hsb;

        public RGB2HSB(int rgb, float[] hsb) {

            this.rgb = rgb;
            this.hsb = hsb;
        }
    }

}
