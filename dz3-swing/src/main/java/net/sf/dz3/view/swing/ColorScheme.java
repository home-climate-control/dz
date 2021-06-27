package net.sf.dz3.view.swing;

import net.sf.dz3.device.model.HvacMode;
import org.apache.logging.log4j.LogManager;

import java.awt.Color;
import java.util.Map;
import java.util.TreeMap;

public class ColorScheme {

    private static final Map<HvacMode, ColorScheme> colorMap = new TreeMap<>();

    public final Color bottom;
    public final Color top;
    public final Color setpoint;
    public final Color setpointChanging;
    public final Color error;
    public final Color off;
    public final Color green;
    public final Color noticeDefault;
    public final Color noticeActive;
    public final Color sensorNormal;
    public final Color sensorStale;
    public final Color sensorError;

    /**
     * Background color.
     *
     * Limitation: it must be the same for all three maps (heating cooling, off), or things will look really funny
     * because some zones may be in heating and some in cooling mode.
     */
    public final Color background;

    public static synchronized ColorScheme getScheme(HvacMode mode) {

        if (colorMap.isEmpty()) {

            colorMap.put(HvacMode.COOLING, coolingMap);
            colorMap.put(HvacMode.OFF, offMap);
            colorMap.put(HvacMode.HEATING, heatingMap);
        }

        if (mode == null) {

            // VT: NOTE: the commend was "For old installations". So old, I don't even
            // remember what's that about.

            LogManager.getLogger(ColorScheme.class).error("mode == null???", new IllegalArgumentException("trace"));
            mode = HvacMode.HEATING;
        }

        return colorMap.get(mode);
    }

    @SuppressWarnings("squid:S107")
    private ColorScheme(
            Color bottom,
            Color top,
            Color setpoint,
            Color setpointChanging,
            Color error,
            Color off,
            Color green,
            Color noticeDefault,
            Color noticeActive,
            Color background,
            Color sensorNormal,
            Color sensorStale,
            Color sensorError) {

        // VT: NOTE: squid:S107 - sorry, dudes, this is unavoidable here.

        this.bottom = bottom;
        this.top = top;
        this.setpoint = setpoint;
        this.setpointChanging = setpointChanging;
        this.error = error;
        this.off = off;
        this.green = green;
        this.noticeDefault = noticeDefault;
        this.noticeActive = noticeActive;
        this.background = background;
        this.sensorNormal = sensorNormal;
        this.sensorStale = sensorStale;
        this.sensorError = sensorError;
    }

    public static final ColorScheme coolingMap = new ColorScheme(
            new Color(10, 70, 240),  // 0A46F0 BLUE
            new Color(240, 70, 10),  // F0460A ORANGE-RED
            new Color(48, 160, 255), // 30A0FF
            Color.YELLOW,
            Color.RED.darker().darker(),
            Color.GRAY,
            Color.GREEN,
            new Color(0x55, 0x55, 0x55), // DARK GRAY
            Color.YELLOW,
            Color.BLACK,
            Color.GREEN.darker(),
            Color.YELLOW,
            Color.RED.darker());

    public static final ColorScheme heatingMap = new ColorScheme(
            new Color(240, 70, 10),  // F0460A ORANGE-RED
            new Color(10, 70, 240),  // 0A46F0 BLUE
            new Color(255, 160, 48), // FFA030
            Color.YELLOW,
            Color.RED.darker().darker(),
            Color.GRAY,
            Color.GREEN,
            new Color(0x55, 0x55, 0x55), // DARK GRAY
            Color.YELLOW,
            Color.BLACK,
            Color.GREEN.darker(),
            Color.YELLOW,
            Color.RED.darker());

    public static final ColorScheme offMap = new ColorScheme(
            Color.GRAY,
            Color.BLACK,
            Color.WHITE,
            Color.YELLOW,
            Color.RED.darker().darker(),
            Color.GRAY,
            Color.GREEN,
            new Color(0x55, 0x55, 0x55), // DARK GRAY
            Color.YELLOW,
            Color.BLACK,
            Color.GREEN.darker(),
            Color.YELLOW,
            Color.RED.darker());
}
