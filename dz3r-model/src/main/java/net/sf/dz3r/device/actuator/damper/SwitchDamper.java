package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.device.actuator.Switch;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.Optional;

/**
 * Damper controlled by a switch.
 *
 * Most existing HVAC dampers are like this, controlled by 24VAC.
 *
 * @param <A> Address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SwitchDamper<A extends Comparable<A>> extends AbstractDamper<A> {

    private static final double DEFAULT_THRESHOLD = 0.5;

    /**
     * Hardware switch that controls the actual damper.
     */
    private final Switch<?> target;

    /**
     * Switch threshold.
     *
     * Values passed to {@link #set(double)} above the threshold
     * will set the switch to 1,values equal or less will set the switch to 0.
     */
    private double threshold;
    /**
     * If {@code} true, then {@code 1.0} damper position would mean the switch in {@code false} state,
     * not in {@code true} like it normally would.
     */
    private final boolean inverted;

    protected SwitchDamper(A address, Switch<?> target) {
        this(address, null, target, DEFAULT_THRESHOLD, null, false);
    }

    protected SwitchDamper(A address, Switch<?> target, double threshold) {
        this(address, null, target, threshold, null, false);
    }

    protected SwitchDamper(A address, Switch<?> target, double threshold, boolean inverted) {
        this(address, null, target, threshold, null, inverted);
    }

    protected SwitchDamper(A address, Switch<?> target, double threshold, Double parkPosition) {
        this(address, null, target, threshold, parkPosition, false);
    }

    protected SwitchDamper(A address, Switch<?> target, double threshold, Double parkPosition, boolean inverted) {
        this(address, null, target, threshold, parkPosition, inverted);
    }

    protected SwitchDamper(A address, Scheduler scheduler, Switch<?> target, double threshold, Double parkPosition, boolean inverted) {
        super(address, scheduler);

        check(target);
        check(threshold);

        this.target = target;
        this.threshold = threshold;
        this.inverted = inverted;

        Optional.ofNullable(parkPosition).ifPresent(this::setParkPosition);
    }

    private void check(Switch<?> target) {
        if (target == null) {
            throw new IllegalArgumentException("target can't be null");
        }
    }

    private void check(double threshold) {
        if (threshold <= 0 || threshold >= 1.0 ) {
            throw new IllegalArgumentException("Unreasonable threshold value given ("
                    + threshold + "), valid values are (0 < threshold < 1)");
        }
    }

    @Override
    public Mono<Double> set(double position) {

        boolean state = (position > threshold) != inverted;

        logger.debug("translated {} => {}{}", position, state, (inverted ? " (inverted)" : ""));

        return Mono.create(sink -> {
                    try {
                        target.setState(state).block();
                        sink.success(position);
                    } catch (Exception ex) {
                        sink.error(ex);
                    }
                }
        );
    }
}
