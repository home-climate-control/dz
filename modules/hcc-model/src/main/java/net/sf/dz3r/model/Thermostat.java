package net.sf.dz3r.model;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.controller.HysteresisController;
import net.sf.dz3r.controller.ProcessController.Status;
import net.sf.dz3r.controller.pid.AbstractPidController;
import net.sf.dz3r.controller.pid.SimplePidController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.CallingStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Clock;

/**
 * Thermostat.
 *
 * Unlike the previous incarnation, just calculates the control signal assuming it is on.
 * The rest will be taken care of elsewhere.
 *
 * Also unlike the previous incarnation, the process controller is inside.
 *
 * But just like the previous incarnation, PID values define the operating mode - positive for cooling, negative
 * for heating. Whoever is changing the operating mode must also change the thermostat PID controller setting as well.
 *
 * VT: FIXME: Provide the means to change the PID controller settings.
 *
 * @see net.sf.dz3r.device.model.Thermostat
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class Thermostat implements Addressable<String> {

    private final Logger logger = LogManager.getLogger();
    private final Clock clock;

    private final String name;
    public final Range<Double> setpointRange;

    /**
     * The current process variable value.
     */
    private Signal<Double, Void> pv;

    /**
     * Hysteresis boundaries for the {@link #signalRenderer}.
     *
     * This value is not to be fiddled with, change {@link #controller}'s P instead.
     */
    private static final double HYSTERESIS = 1.0;

    /**
     * Controller defining this thermostat's dynamic behavior.
     */
    private final AbstractPidController<Void> controller;

    /**
     * Controller defining this thermostat's output signal.
     */
    private final HysteresisController<Status<Double>> signalRenderer;

    private final Sinks.Many<Signal<Double, Status<Double>>> stateSink = Sinks.many().unicast().onBackpressureBuffer();
    private final Flux<Signal<Double, Status<Double>>> stateFlux = stateSink.asFlux();

    /**
     * Create a thermostat with a default 10C..40C setpoint range and specified setpoint and PID values.
     *
     */
    public Thermostat(String name, Double setpoint, double p, double i, double d, double limit) {
        this(Clock.systemUTC(), name, new Range<>(10d, 40d), setpoint, p, i, d, limit);
    }

    /**
     * Create a thermostat with a custom setpoint range.
     *
     * @param clock Clock to base operations on.
     * @param name Thermostat name.
     * @param setpointRange Setpoint range for this thermostat.
     * @param setpoint Initial setpoint.
     * @param p PID controller proportional weight.
     * @param i PID controller integral weight.
     * @param d PID controller derivative weight.
     * @param limit PID controller saturation limit.
     */
    public Thermostat(Clock clock, String name, Range<Double> setpointRange, Double setpoint, double p, double i, double d, double limit) {

        this.clock = HCCObjects.requireNonNull(clock, "clock can't be null");
        this.name = name;
        this.setpointRange = setpointRange;

        controller = new SimplePidController<>("(controller) " + name, setpoint, p, i, d, limit);
        signalRenderer = new HysteresisController<>("(signalRenderer) " + name, 0, HYSTERESIS);
    }

    /**
     * Get the human readable thermostat name.
     *
     * @return Thermostat name.
     */
    @Override
    public String getAddress() {
        return name;
    }

    private void configurationChanged() {
        // VT: FIXME: Recalculate everything, issue new control signal
    }

    public void setSetpoint(Double setpoint) {

        if (setpoint != null && !setpointRange.contains(setpoint)) {
            throw new IllegalArgumentException(setpoint + " is outside of " + setpointRange.min + ".." + setpointRange.max);
        }

        controller.setSetpoint(setpoint);

        logger.info("setSetpoint({}): {}", name, setpoint);

        configurationChanged();
    }

    public Double getSetpoint() {
        return controller.getSetpoint();
    }

    /**
     * Compute the thermostat status flux.
     *
     * @see net.sf.dz3r.device.actuator.economizer.v2.PidEconomizer#computeDeviceState(Flux)
     */
//    @Override
    public Flux<Signal<Status<CallingStatus>, Void>> compute(Flux<Signal<Double, Void>> pv) {

        // Compute the control signal to feed to the renderer.
        // Might want to make this available to outside consumers for instrumentation.
        var stage1 = controller
                .compute(pv)
                .doOnNext(e -> logger.trace("controller/{}: {}", name, e))
                .doOnComplete(stateSink::tryEmitComplete); // or it will hang forever

        // Discard things the renderer doesn't understand.
        // The PID controller output value becomes the extra payload to pass to the zone controller to calculate demand.
        Flux<Signal<Double, Status<Double>>> stage2 = stage1
                .map(s -> new Signal<>(s.timestamp, s.getValue().signal, s.getValue(), s.status, s.error));

        // Inject signals from raise(), if any
        var stage3 = Flux.merge(stage2, stateFlux);

        // Deliver the signal
        // Might want to expose this as well
        return signalRenderer
                .compute(stage3)
                .doOnNext(e -> logger.trace("renderer/{}: {}", name, e))
                .map(this::mapOutput);
    }

    private Signal<Status<CallingStatus>, Void> mapOutput(Signal<Status<Double>, Status<Double>> source) {

        var sample = source.getValue() instanceof HysteresisController.HysteresisStatus value
                ? value.sample
                : null;

        // Watch for the error signal
        var demand = source.isError() ? 0 : source.payload.signal - signalRenderer.getThresholdLow();
        var calling = !source.isError() && Double.compare(source.getValue().signal, 1.0) == 0;

        return new Signal<>(
                source.timestamp,
                new Status<>(source.payload.setpoint, source.payload.error, new CallingStatus(sample, demand, calling)),
                null,
                source.status,
                source.error);
    }

    /**
     * Make the thermostat reconsider its calling status.
     *
     * If it is calling, there must be no change.
     * If it is not calling, but the signal is within the hysteresis loop, status must change to calling.
     * If it is not calling, and the signal is below the hysteresis loop, there must be no change.
     */
    public void raise() {
        ThreadContext.push("raise");
        try {
            var actual = signalRenderer.getProcessVariable();

            if (actual == null) {
                logger.debug("no renderer state available yet, we'll have to wait until it is established");
                return;
            }

            if (actual.getValue() >= HYSTERESIS) {
                // no need, it's already calling
                return;
            }

            if (actual.getValue() < -HYSTERESIS) {
                // no point, it will not get high enough
                return;
            }

            // All we need is a little nudge
            // Timestamp has to be current - there's no say how long the PV was sitting there
            var adjusted = new Signal<>(clock.instant(), HYSTERESIS, actual.payload);

            logger.trace("{}: actual:   {}", getAddress(), actual);
            logger.trace("{}: adjusted: {}", getAddress(), adjusted);

            stateSink.tryEmitNext(adjusted);

        } finally {
            ThreadContext.pop();
        }
    }
}
