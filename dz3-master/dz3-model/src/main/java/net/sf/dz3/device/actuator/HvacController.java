package net.sf.dz3.device.actuator;

import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.HvacSignal;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.UnitSignal;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxAttribute;

/**
 * Whereas {@link Unit} is a control logic abstraction, the class implementing this interface
 * issues commands to the actual {@link HvacDriver HVAC unit device driver}.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public interface HvacController extends DataSink<UnitSignal>, DataSource<HvacSignal>, Comparable<HvacController> {

    /**
     * Get the name of the unit.
     */
    @JmxAttribute(description="Unit name")
    String getName();

    /**
     * @return Current operating mode.
     */
    @JmxAttribute(description="Current mode")
    HvacMode getMode();
    
    /**
     * Set operating mode.
     *  
     * @param mode Mode to set.
     */
    void setMode(HvacMode mode);
}
