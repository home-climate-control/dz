package net.sf.dz3r.device.actuator.damper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Common functionality for all dampers.
 *
 * @param <A> Address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractDamper<A extends Comparable<A>> implements Damper<A> {

    protected final Logger logger = LogManager.getLogger();

    private final A address;
    protected final Scheduler scheduler;

    /**
     * A damper position defined as 'parked'.
     *
     * Default is {@code null} - none. See commits related to https://github.com/home-climate-control/dz/issues/51
     * for more details.
     *
     * If the value is {@code null} and {@link #park()} method is called, the value of
     * {@link #DEFAULT_PARK_POSITION} is used.
     */
    private Double parkPosition = null;

    /**
     * Create an instance with a default scheduler.
     *
     * @param address Damper address.
     */
    protected AbstractDamper(A address) {
        this(address, null);
    }

    /**
     * Create an instance with a given scheduler.
     *
     * @param address Damper address.
     * @param scheduler Scheduler to use. {@code null} means using {@link Schedulers#newSingle(String, boolean)}.
     */
    protected AbstractDamper(A address, Scheduler scheduler) {
        if (address == null) {
            throw new IllegalArgumentException("address can't be null");
        }

        this.address = address;
        this.scheduler = scheduler == null ? Schedulers.newSingle("damper:" + address, true) : scheduler;

    }

    @Override
    public final A getAddress() {
        return address;
    }

    @Override
    public final void setParkPosition(double parkPosition) {

        if (parkPosition < 0 || parkPosition > 1) {
            throw new IllegalArgumentException("Invalid position (" + parkPosition + ") - value can be 0..1.");
        }

        this.parkPosition = parkPosition;
    }

    @Override
    public final double getParkPosition() {
        return parkPosition == null ? DEFAULT_PARK_POSITION : parkPosition;
    }

    @Override
    public Mono<Double> park() {
        logger.debug("park()");
        return set(getParkPosition());
    }

    protected final void checkPosition(double position) {
        if ( position < 0 || position > 1.0 || Double.compare(position, Double.NaN) == 0) {

            throw new IllegalArgumentException("Position out of 0...1 range: " + position);
        }
    }
}
