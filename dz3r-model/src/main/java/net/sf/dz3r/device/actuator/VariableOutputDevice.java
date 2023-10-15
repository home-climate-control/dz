package net.sf.dz3r.device.actuator;

import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

/**
 * Variable output device.
 *
 * This specification honors <a href="https://martinfowler.com/bliki/CQRS.html">Command Query Responsibility Segregation</a>.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface VariableOutputDevice extends AutoCloseable {

    /**
     * Command to pass to the device.
     *
     * @param on Whether to turn the device on or off.
     * @param output Output power, {@code 0.0} to {@code 1.0} inclusive.
     */
    record Command(
            boolean on,
            double output
    ) {

    }

    public record OutputState(
            Boolean on,
            Double output
    ) {

    }

    /**
     * Get the instant state of the device.
     *
     * @return Actual, current state of the device, immediately.
     */
    DeviceState<OutputState> getState();

    /**
     * Request the provided state, return immediately.
     *
     * If the device is not {@link #isAvailable() available}, the command is still accepted.
     *
     * @param on Request the device to be on, or off.
     * @param output Request the output, {@code 0.0} to {@code 1.0}.
     *
     * @return The result of {@link #getState()} after the command was accepted (not executed).
     */
    DeviceState<OutputState> setState(boolean on, double output);

    /**
     * Check the reported device availability.
     *
     * @return previously reported device availability, immediately.
     */
    boolean isAvailable();

    /**
     * Get the state change notification flux.
     *
     * Note that this sink will NOT emit error signals, however, {@link DeviceState#available} will reflect device availability
     * the moment the signal was emitted.
     *
     * @return Flux emitting signals every time the {@link #setState(boolean, double) requested} or {@link #getState() actual} state has changed.
     */
    Flux<Signal<DeviceState<OutputState>, String>> getFlux();

    /**
     * Close the device, synchronously.
     */
    @Override
    void close() throws Exception;
}
