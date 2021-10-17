package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.DeviceState;
import reactor.core.publisher.Mono;

/**
 * Damper abstraction.
 *
 * @param <A> Address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public interface Damper<A extends Comparable<A>> extends Addressable<A> {

    public class State extends DeviceState<Double> {
        public State(Double requested, Double actual) {
            super(requested, actual);
        }
    }

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
     * Set the damper opening.
     *
     * This method is intentionally not made available to JMX instrumentation,
     * to avoid interference.
     *
     * @param position 0 is fully closed, 1 is fully open, 0...1 corresponds
     * to partially open position.
     *
     * @return A Mono that is completed when the state is actually set.
     *
     * @exception IllegalArgumentException if {@code position} is outside of 0...1 range.
     */
    Mono<Double> set(double position);

    /**
     * Position to park if there was no park position {@link #setParkPosition(double) explicitly specified}.
     *
     * Normally, the damper should be fully open in this position.
     */
    public static final double DEFAULT_PARK_POSITION = 1.0;

    /**
     * Get the 'safe' position.
     *
     * @return Damper position that is considered 'parked'. Recommended
     * default value is 1 (fully open).
     */
    double getParkPosition();

    /**
     * 'Park' the damper.
     *
     * This call will cause the damper to move to {@link #getParkPosition
     * parked position}. Any subsequent call to {@link #set set()} will
     * unpark the damper. Any subsequent call to this method must cause no change,
     * the implementation is expected to be idempotent.
     *
     * A damper is parked in two cases: first, when the HVAC unit stops (so
     * the ventilation system can continue to work), second, when DZ shuts
     * down, so DZ can be safely disconnected and the HVAC infrastructure
     * can work without DZ's interference.
     *
     * @return A Mono that is completed when the damper is parked (it
     * may take a while if the damper is configured with a transition
     * controller).
     */
    Mono<Double> park();
}
