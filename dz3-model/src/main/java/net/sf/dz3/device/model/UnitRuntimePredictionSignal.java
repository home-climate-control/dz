package net.sf.dz3.device.model;

import java.time.Duration;
import java.time.Instant;

public class UnitRuntimePredictionSignal extends HvacSignal {

    /**
     * Calculated efficiency.
     */
    public final double k;

    /**
     * How much time is left until the unit is expected to satisfy the demand.
     *
     * {@code null} value indicates that the prediction can't be made at this point.
     */
    public final Duration left;

    /**
     * Moment in time when the unit is expected to satisfy the demand.
     *
     * {@code null} value indicates that the prediction can't be made at this point.
     */
    public final Instant arrival;

    public UnitRuntimePredictionSignal(
            HvacSignal hvacSignal,
            double k,
            Duration left, Instant arrival) {
        super(hvacSignal.mode, hvacSignal.demand, hvacSignal.running, hvacSignal.uptime);

        this.k = k;
        this.left = left;
        this.arrival = arrival;
    }
}