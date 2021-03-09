package net.sf.dz3.device.model;

import net.sf.dz3.device.actuator.Damper;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
 * A damper controller.
 *
 * Controls the behavior of the dampers belonging to a particular {@link
 * Unit unit} based on the inputs from the {@link ZoneController zone
 * controller} and manual fan override.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
 */
public interface DamperController extends DataSink<UnitSignal> {

    /**
     * Create an association between the thermostat and the damper.
     *
     * @param ts Thermostat to use as a key.
     *
     * @param damper Damper to use as a value.
     */
    void put(Thermostat ts, Damper damper);
    
    /**
     * Remove the association between the given thermostat and whatever damper it was associated with.
     * 
     * @param ts Thermostat to remove the association for.
     */
    void remove(Thermostat ts);
    
    /**
     * Disable the controller and put all dampers into parked position, synchronously.
     */
    void powerOff();
}
