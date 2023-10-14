package net.sf.dz3r.device.actuator;

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

    /**
     * Variable output device state.
     *
     * @param id device ID.
     * @param available {@code true} if the device was reported as available, {@code false} if not, or if it is unknown.
     * @param requested Requested device state.
     * @param actual Actual device state.
     * @param queueDepth Current value of send queue depth.
     */
    public record State(
            String id,
            boolean available,
            OutputState requested,
            OutputState actual,
            int queueDepth
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
    State getState();

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
    State setState(boolean on, double output);

    /**
     * Check the reported device availability.
     *
     * @return previously reported device availability, immediately.
     */
    boolean isAvailable();

    /**
     * Get the state change notification flux.
     *
     * Note that this sink will NOT emit error signals, however, {@link State#available()} will reflect device availability
     * the moment the signal was emitted.
     *
     * @return Flux emitting signals every time the {@link #setState(boolean, double) requested} or {@link #getState() actual} state has changed.
     */
    Flux<Signal<State, String>> getFlux();

    /**
     * Close the device, synchronously.
     */
    @Override
    void close() throws Exception;
}
