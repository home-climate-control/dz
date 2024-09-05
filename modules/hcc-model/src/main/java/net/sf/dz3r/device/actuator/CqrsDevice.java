package net.sf.dz3r.device.actuator;

import net.sf.dz3r.device.DeviceState;
import com.homeclimatecontrol.hcc.signal.Signal;
import reactor.core.publisher.Flux;

/**
 * Common interface for devices supporting <a href="https://martinfowler.com/bliki/CQRS.html">Command Query Responsibility Segregation</a> pattern.
 *
 * @param <I> Command type.
 * @param <O> Output type. *
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public interface CqrsDevice<I, O> extends AutoCloseable {

    /**
     * Check the reported device availability.
     *
     * @return previously reported device availability, immediately.
     */
    boolean isAvailable();

    /**
     * Get the instant state of the device.
     *
     * @return Actual, current state of the device, immediately.
     */
    DeviceState<O> getState();

    /**
     * Request the provided state, return immediately.
     *
     * If the device is not {@link #isAvailable() available}, the command is still accepted.
     *
     * @param state State to set.
     *
     * @return The result of {@link #getState()} after the command was accepted (not executed).
     */
    DeviceState<O> setState(I state);

    /**
     * Get the state change notification flux.
     *
     * Note that this sink will NOT emit error signals, however, {@link DeviceState#available} will reflect device availability
     * the moment the signal was emitted.
     *
     * @return Flux emitting signals every time the {@link #setState requested} or {@link #getState() actual} state has changed.
     * Signal payload is the device identifier assigned upon assembly.
     */
    Flux<Signal<DeviceState<O>, String>> getFlux();
    /**
     * Close the device, synchronously.
     */
    @Override
    void close() throws Exception;
}
