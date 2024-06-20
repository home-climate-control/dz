package net.sf.dz3r.controller;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.signal.Signal;

import java.time.Duration;
import java.time.Instant;

/**
 * Emits signal decaying according to radioactive decay algorithm on input signal change.
 *
 * Reference: <a href="https://www.omnicalculator.com/chemistry/half-life#half-life-formula">half life formulas</a>.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class HalfLifeController<P>  extends AbstractProcessController<Double, Double, P> {

    private Duration halfLife;

    /**
     * Last known process variable value.
     */
    private Signal<Double, P> lastPV;

    /**
     * Current calculation baseline.
     */
    private Sample current;

    /**
     * Create an instance.
     *
     * @param jmxName  This controller's JMX name.
     * @param halfLife Duration required for the output value to halve with no additional input changes.
     */
    protected HalfLifeController(String jmxName, Duration halfLife) {
        super(jmxName, 0.0);

        setHalfLife(halfLife);

        logger.debug("Created {} with halfLife={}", jmxName, halfLife);
    }

    @Override
    protected double getError(Signal<Double, P> pv, Double setpoint) {
        throw new UnsupportedOperationException("Doesn't make sense for this class. Reimplement as independent and not AbstractProcessController child?");
    }

    @Override
    protected synchronized Signal<Status<Double>, P> wrapCompute(Double setpoint, Signal<Double, P> pv) {

        // This will only be non-null upon second invocation
        var lastOutputSignal = getLastOutputSignal();

        try {

            if (lastOutputSignal == null) {

                // Not much we can do, this is the first signal ever. The superclass will remember it for us.
                return new Signal<>(pv.timestamp, new Status<>(setpoint, 0d, 0d), pv.payload);
            }

            double diff = getDiff(lastPV, pv);

            return new Signal<>(
                    pv.timestamp,
                    new Status<>(setpoint, diff, computeRemaining(pv.timestamp, lastOutputSignal.getValue().signal, diff)),
                    pv.payload
            );

        } finally {

            // Need to remember the last PV for the next round - but *after* we're done with calculations
            this.lastPV = pv;
        }
    }

    /**
     * Compute the remaining value.
     *
     * @param timestamp Moment in time for which the remaining value needs to be calculated.
     * @param lastKnown Last known remaining value.
     * @param diff Difference between last known and current value.
     *
     * @return Remaining value.
     */
    private double computeRemaining(Instant timestamp, double lastKnown, double diff) {

        if (Double.compare(diff, 0) != 0 || current == null) {

            // There's been a change, we'll need to recalculate things
            current = new Sample(timestamp, lastKnown + diff);
        }

        return computeRemaining(current.value, Duration.between(current.start, timestamp));
    }

    private double computeRemaining(double initial, Duration elapsed) {

        // Duration.dividedBy() doesn't work here
        var power = ((double) elapsed.toMillis() / halfLife.toMillis());

        return initial * Math.pow(0.5, power);
    }

    private double getDiff(Signal<Double, P> before, Signal<Double, P> now) {

        if (before == null || before.isError() || now.isError()) {
            return 0;
        }

        return now.getValue() - before.getValue();
    }

    @Override
    protected void configurationChanged() {
        // No need to do anything, new values will take effect upon next input signal arrival
    }

    @Override
    public void setSetpoint(Double setpoint) {

        // VT: NOTE: Can't throw an exception unconditionally because setSetpoint is called from the constructor;
        // this can be addressed in Java22 where variables can be assigned before calling super, but for now this will do

        if (setpoint == null || Double.compare(setpoint, 0) != 0) {
            throw new IllegalArgumentException("this operation doesn't make sense for this controller and will break process control");
        }

        super.setSetpoint(0.0);
    }

    private void setHalfLife(Duration halfLife) {
        HCCObjects.requireNonNull(halfLife, "halfLife can't be null");

        if (halfLife.isNegative() || halfLife.isZero()) {
            throw new IllegalArgumentException("halfLife must be positive");
        }

        this.halfLife = halfLife;
    }

    private record Sample(
            Instant start,
            double value
    ) {

    }
}
