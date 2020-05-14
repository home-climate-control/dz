package net.sf.dz3.view.swing;

import java.awt.Color;
import java.util.Map;
import java.util.TreeMap;

import net.sf.dz3.device.model.HvacMode;

public class ColorScheme {

    private static Map<HvacMode, ColorScheme> colorMap = new TreeMap<HvacMode, ColorScheme>();

    public final Color BOTTOM;
    public final Color TOP;
    public final Color SETPOINT;
    public final Color SETPOINT_CHANGING;
    public final Color ERROR;
    public final Color OFF;
    public final Color GREEN;
    public final Color NOTICE_DEFAULT;
    public final Color NOTICE_ACTIVE;

    /**
     * Background color.
     *
     * Limitation: it must be the same for all three maps (heating cooling, off), or things will look really funny
     * because some zones may be in heating and some in cooling mode.
     */
    public final Color BACKGROUND;

    public synchronized static ColorScheme getScheme(HvacMode mode) {

        if (colorMap.isEmpty()) {

            colorMap.put(HvacMode.COOLING, coolingMap);
            colorMap.put(HvacMode.OFF, offMap);
            colorMap.put(HvacMode.HEATING, heatingMap);
        }

        if (mode == null) {

            // VT: FIXME: For old installations
            mode = HvacMode.HEATING;
        }

        return colorMap.get(mode);
    }

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
    		Color background) {

        this.BOTTOM = bottom;
        this.TOP = top;
        this.SETPOINT = setpoint;
        this.SETPOINT_CHANGING = setpointChanging;
        this.ERROR = error;
        this.OFF = off;
        this.GREEN = green;
        this.NOTICE_DEFAULT = noticeDefault;
        this.NOTICE_ACTIVE = noticeActive;
        this.BACKGROUND = background;
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
            Color.BLACK);

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
            Color.BLACK);

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
            Color.BLACK);
}
