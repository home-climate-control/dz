package net.sf.dz3r.model;

import net.sf.dz3r.controller.HysteresisController;
import net.sf.dz3r.controller.ProcessController;
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
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class Thermostat implements ProcessController<Double, CallingStatus, Void>, Addressable<String> {

    private final Logger logger = LogManager.getLogger();

    private final String name;
    public final Range<Double> setpointRange;

    /**
     * Thermostat setpoint.
     */
    private double setpoint;

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
    public Thermostat(String name, double setpoint, double p, double i, double d, double limit) {
        this(name, new Range<>(10d, 40d), setpoint, p, i, d, limit);
    }

    /**
     * Create a thermostat with a custom setpoint range.
     *
     * @param name Thermostat name.
     * @param setpointRange Setpoint range for this thermostat.
     * @param setpoint Initial setpoint.
     * @param p PID controller proportional weight.
     * @param i PID controller integral weight.
     * @param d PID controller derivative weight.
     * @param limit PID controller saturation limit.
     */
    public Thermostat(String name, Range<Double> setpointRange, double setpoint, double p, double i, double d, double limit) {

        this.name = name;
        this.setpointRange = setpointRange;
        this.setpoint = setpoint;

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

    @Override
    public void setSetpoint(double setpoint) {

        if (!setpointRange.contains(setpoint)) {
            throw new IllegalArgumentException(setpoint + " is outside of " + setpointRange.min + ".." + setpointRange.max);
        }

        this.setpoint = setpoint;
        controller.setSetpoint(setpoint);

        logger.info("setSetpoint({}): {}", name, setpoint);

        configurationChanged();
    }

    @Override
    public double getSetpoint() {
        return setpoint;
    }

    @Override
    public Signal<Double, Void> getProcessVariable() {
        return pv;
    }

    @Override
    public double getError() {

        if (pv == null) {
            // No sample, no error
            return 0;
        }

        return pv.getValue() - setpoint;
    }

    /**
     * Compute the thermostat status flux.
     *
     * @see net.sf.dz3r.device.actuator.economizer.v2.PidEconomizer#computeDeviceState(Flux)
     */
    @Override
    public Flux<Signal<Status<CallingStatus>, Void>> compute(Flux<Signal<Double, Void>> pv) {

        // Compute the control signal to feed to the renderer.
        // Might want to make this available to outside consumers for instrumentation.
        var stage1 = controller
                .compute(pv)
                .doOnNext(e -> logger.trace("controller/{}: {}", name, e))
                .doOnComplete(stateSink::tryEmitComplete); // or it will hang forever

        // Discard things the renderer doesn't understand.
        // Total failure is denoted by NaN by stage 1, it will get through.
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
        var demand = source.payload.signal - signalRenderer.getThresholdLow();
        var calling = Double.compare(source.getValue().signal, 1.0) == 0;

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
            var adjusted = new Signal<>(actual.timestamp, HYSTERESIS, actual.payload);

            logger.trace("{}: actual:   {}", getAddress(), actual);
            logger.trace("{}: adjusted: {}", getAddress(), adjusted);

            stateSink.tryEmitNext(adjusted);

        } finally {
            ThreadContext.pop();
        }
    }
}
