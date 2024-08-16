package net.sf.dz3r.device.actuator.economizer.v2;

import net.sf.dz3r.controller.HysteresisController;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.controller.pid.AbstractPidController;
import net.sf.dz3r.controller.pid.SimplePidController;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.economizer.AbstractEconomizer;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.model.Thermostat;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;

/**
 * Economizer implementation with PID jitter control.
 *
 * More information: <a href="https://github.com/home-climate-control/dz/wiki/HVAC-Device:-Economizer">HVAC Device: Economizer</a>
 *
 * @param <A> Actuator device address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class PidEconomizer<A extends Comparable<A>> extends AbstractEconomizer {

    /**
     * Controller defining this economizer's dynamic behavior.
     */
    private final AbstractPidController<Void> controller;

    /**
     * Hysteresis boundaries for the {@link #signalRenderer}.
     *
     * This value is not to be fiddled with, change {@link #controller}'s P instead.
     */
    private static final double HYSTERESIS = 1.0;

    /**
     * Controller defining this economizer's output signal.
     */
    private final HysteresisController<ProcessController.Status<Double>> signalRenderer;
    /**
     * Create an instance.
     *
     * Note that only the {@code ambientFlux} argument is present, indoor flux is provided to {@link #compute(Flux)}.
     *
     * @param ambientFlux Flux from the ambient temperature sensor.
     * @param device HVAC device acting as the economizer.
     * @param timeout Stale timeout. 90 seconds is a reasonable default.
     */
    public PidEconomizer(
            Clock clock,
            String name,
            EconomizerConfig settings,
            Flux<Signal<Double, Void>> ambientFlux,
            HvacDevice device,
            Duration timeout) {

        super(clock, name, settings, device, timeout);

        controller = new SimplePidController<>("(controller) " + getAddress(), 0d, settings.P, settings.I, 0, settings.saturationLimit);
        signalRenderer = new HysteresisController<>("(signalRenderer) " + getAddress(), 0, HYSTERESIS);

        initFluxes(ambientFlux);
    }

    /**
     * Compute the {@link #device} state flux.
     *
     * @see Thermostat#compute(Flux)
     */
    @Override
    protected Flux<Signal<Boolean, ProcessController.Status<Double>>> computeDeviceState(Flux<Signal<Double, Void>> pv) {

        // Compute the control signal to feed to the renderer.
        // Might want to make this available to outside consumers for instrumentation.
        var stage1 = controller
                .compute(pv)
                .doOnNext(e -> logger.debug("controller/{}: {}", getAddress(), e));

        // Interpret things the renderer doesn't understand
        var stage2 = stage1.map(this::computeRendererInput);

        // Deliver the signal
        // Might want to expose this as well
        return signalRenderer
                .compute(stage2)
                .doOnNext(e -> logger.debug("renderer/{}: {}", getAddress(), e))
                .map(this::mapOutput);
    }

    /**
     * Convert a possibly error signal from the computing pipeline into an actionable signal for the hysteresis controller.
     */
    private Signal<Double, ProcessController. Status<Double>> computeRendererInput(Signal<ProcessController.Status<Double>, Void> signal) {

        if (signal.isOK()) {
            // PID controller output value becomes the extra payload but is ignored at the moment (unlike Thermostat#compute()).
            return new Signal<>(signal.timestamp, signal.getValue().signal, signal.getValue(), signal.status, signal.error);
        }

        // Any kind of errors at this point must be interpreted as "turn it off"
        return new Signal<>(signal.timestamp, -1d);
    }

    private Signal<Boolean, ProcessController.Status<Double>> mapOutput(Signal<ProcessController.Status<Double>, ProcessController.Status<Double>> source) {

        var calling = Double.compare(source.getValue().signal, 1.0) == 0;

        return new Signal<>(source.timestamp, calling, source.getValue());
    }
}
