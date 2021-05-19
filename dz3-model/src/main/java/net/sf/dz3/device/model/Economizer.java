package net.sf.dz3.device.model;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;

/**
 * An abstraction for the economizer.
 * 
 * @see http://diy-zoning.sourceforge.net/Homeowners/faq.html#economizer
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2010
 */
public interface Economizer extends DataSource<Double> {

    /**
     * @return How many degrees Celsius the outdoor temperature should be above (for heating)
     * or below (for cooling) the indoor temperature in order for the economizer to be activated.
     * 
     * Threshold value must be non-negative, implementation is expected to figure out whether the instance
     * is used for heating or cooling.
     */
    @JmxAttribute(description = "Difference between indoor and outdoor temperature necessary for triggering")
    double getThreshold();
}
