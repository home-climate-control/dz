package net.sf.dz3r.model;

import net.sf.dz3r.controller.HysteresisController;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.controller.pid.AbstractPidController;
import net.sf.dz3r.controller.pid.SimplePidController;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ThermostatStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

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
 * @see net.sf.dz3.device.model.Thermostat
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class Thermostat implements ProcessController<Double, ThermostatStatus, Void>, Addressable<String> {

    private final Logger logger = LogManager.getLogger();

    private final String name;
    private final Range<Double> setpointRange;

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
    public Flux<Signal<Status<ThermostatStatus>, Void>> compute(Flux<Signal<Double, Void>> pv) {

        logger.debug("compute()");

        // Compute the control signal to feed to the renderer.
        // Might want to make this available to outside consumers for instrumentation.
        var stage1 = controller
                .compute(pv)
                .doOnNext(e -> logger.trace("controller/{}: {}", name, e));

        // Discard things the renderer doesn't understand.
        // Total failure is denoted by NaN by stage 1, it will get through.
        // The PID controller output value becomes the extra payload to pass to the zone controller to calculate demand.
        var stage2 = stage1
                .map(s -> new Signal<>(s.timestamp, s.getValue().signal, s.getValue(), s.status, s.error));

        // Deliver the signal
        // Might want to expose this as well
        return signalRenderer
                .compute(stage2)
                .doOnNext(e -> logger.trace("renderer/{}: {}", name, e))
                .map(this::mapOutput);
    }

    private Signal<Status<ThermostatStatus>, Void> mapOutput(Signal<Status<Double>, Status<Double>> source) {

        var demand = source.payload.signal - signalRenderer.getThresholdLow();
        var calling = Double.compare(source.getValue().signal, 1.0) == 0;

        return new Signal<>(
                source.timestamp,
                new Status<>(source.payload.setpoint, source.payload.error, new ThermostatStatus(demand, calling)),
                null,
                source.status,
                source.error);
    }
}
