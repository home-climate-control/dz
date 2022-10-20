package net.sf.dz3r.device.actuator.economizer.v2;

import net.sf.dz3r.controller.HysteresisController;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.controller.pid.AbstractPidController;
import net.sf.dz3r.controller.pid.SimplePidController;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.device.actuator.economizer.AbstractEconomizer;
import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import net.sf.dz3r.model.Thermostat;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;

/**
 * Economizer implementation with PID jitter control.
 *
 * More information: <a href="https://github.com/home-climate-control/dz/wiki/HVAC-Device:-Economizer">HVAC Device: Economizer</a>
 *
 * @param <A> Actuator device address type.
 */
public class PidEconomizer<A extends Comparable<A>> extends AbstractEconomizer<A> {

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
     * @param targetDevice Switch to control the economizer actuator.
     */
    protected PidEconomizer(
            EconomizerSettings settings,
            Flux<Signal<Double, Void>> ambientFlux,
            Switch<A> targetDevice) {

        super(settings, ambientFlux, targetDevice);

        controller = new SimplePidController<>("(controller) " + getAddress(), 0, settings.P, settings.I, 0, settings.saturationLimit);
        signalRenderer = new HysteresisController<>("(signalRenderer) " + getAddress(), 0, HYSTERESIS);

        initFluxes(ambientFlux);
    }

    /**
     * Compute the {@link #targetDevice} state flux.
     *
     * @see Thermostat#compute(Flux)
     */
    @Override
    protected Flux<Signal<Boolean, Void>> computeDeviceState(Flux<Signal<Double, Void>> pv) {

        ThreadContext.push("computeDeviceState");

        try {

            // Compute the control signal to feed to the renderer.
            // Might want to make this available to outside consumers for instrumentation.
            var stage1 = controller
                    .compute(pv)
                    .doOnNext(e -> logger.debug("controller/{}: {}", getAddress(), e));

            // Discard things the renderer doesn't understand.
            // Total failure is denoted by NaN by stage 1, it will get through.
            // The PID controller output value becomes the extra payload but is ignored at the moment (unlike Thermostat#compute()).
            var stage2 = stage1
                    .map(s -> new Signal<>(s.timestamp, s.getValue().signal, s.getValue(), s.status, s.error));

            // Deliver the signal
            // Might want to expose this as well
            return signalRenderer
                    .compute(stage2)
                    .doOnNext(e -> logger.debug("renderer/{}: {}", getAddress(), e))
                    .map(this::mapOutput);

        } finally {
            ThreadContext.pop();
        }
    }

    private Signal<Boolean, Void> mapOutput(Signal<ProcessController.Status<Double>, ProcessController.Status<Double>> source) {

        var calling = Double.compare(source.getValue().signal, 1.0) == 0;

        return new Signal<>(source.timestamp, calling);
    }
}
