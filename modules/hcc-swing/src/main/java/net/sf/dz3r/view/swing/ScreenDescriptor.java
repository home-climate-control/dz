package net.sf.dz3r.view.swing;

import java.awt.Dimension;
import java.awt.Font;

/**
 * Object defining screen element dimensions for a screen of a given size.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class ScreenDescriptor {

	/**
	 * Screen size acronym, per http://en.wikipedia.org/wiki/Graphic_display_resolutions
	 */
    public final String name;

    /**
     * Total display size.
     */
    public final Dimension displaySize;

    public final Font fontCurrentTemperatureC;
    public final Font fontCurrentTemperatureF;
    public final Font fontSetpoint;

    public ScreenDescriptor(String name, Dimension displaySize,
                            Font fontCurrentTemperatureC,
                            Font fontCurrentTemperatureF,
                            Font fontSetpoint) {

        this.name = name;
        this.displaySize = displaySize;
        this.fontCurrentTemperatureC = fontCurrentTemperatureC;
        this.fontCurrentTemperatureF = fontCurrentTemperatureF;
        this.fontSetpoint = fontSetpoint;
    }
}
