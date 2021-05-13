package net.sf.dz3.device.actuator;

import java.io.IOException;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.sem.ACT;

/**
 * The damper abstraction.
 *
 * Classes implementing this interface control the hardware.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
 */
public interface Damper extends DataSink<Double>, DataSource<Double>, JmxAware {
    
    /**
     * Get damper name.
     * 
     * @return Damper name.
     */
    String getName();

    /**
     * Set the damper opening.
     *
     * This method is intentionally not made available to JMX instrumentation,
     * to avoid interference.
     * 
     * @param position 0 is fully closed, 1 is fully open, 0...1 corresponds
     * to partially open position.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware.
     * 
     * @exception IllegalArgumentException if {@code position} is outside of 0...1 range.
     */
    void set(double position) throws IOException;
    
    /**
     * Get current damper position.
     *
     * @return Damper position.
     *
     * @exception IOException if there was a problem communicating with the
     * hardware.
     */
    @JmxAttribute(description = "Current position")
    double getPosition() throws IOException;
    
    /**
     * Set 'park' position.
     *
     * See <a
     * href="http://sourceforge.net/tracker/index.php?func=detail&aid=916345&group_id=52647&atid=467669">bug
     * #916345</a> for more information.
     *
     * <p>
     *
     * This call doesn't cause the damper position to change, it only sets
     * the parked position preference.
     *
     * @param position A value that is considered 'parked'.
     *
     * @see #park
     * 
     * @exception IllegalArgumentException if {@code position} is outside of 0...1 range.
     */
    void setParkPosition(double position);
    
    /**
     * Get 'safe' position.
     *
     * @return A damper position that is considered 'parked'. Recommended
     * default value is 1 (fully open).
     */
    @JmxAttribute(description = "Parked position")
    double getParkPosition();
    
    /**
     * 'Park' the damper.
     *
     * This call will cause the damper to move to {@link #getParkPosition
     * parked position}. Any subsequent call to {@link #set set()} will
     * unpark the damper.
     *
     * <p>
     *
     * A damper is parked in two cases: first, when the HVAC unit stops (so
     * the ventilation system can continue to work), second, when CORE shuts
     * down, so DZ can be safely disconnected and the HVAC infrastructure
     * can work without DZ's interference.
     *
     * <p>
     *
     * VT: NOTE: As ventilation aspect of DZ continues to evolve (talk to
     * Jerry Scharf), the dampers will not be parked when HVAC is shut down;
     * rather, they will be controlled by DZ's ventilation subsystem.
     *
     * @return A semaphore that is triggered when the damper is parked (it
     * may take a while if the damper is configured with a transition
     * controller).
     */
    ACT park();
}
