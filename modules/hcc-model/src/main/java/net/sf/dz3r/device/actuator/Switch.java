package net.sf.dz3r.device.actuator;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Single channel switch abstraction.
 *
 * @param <A> Address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public interface Switch<A extends Comparable<A>> extends Addressable<A> {

    public class State extends DeviceState<Boolean> {
        public State(String id, Boolean available, Boolean requested, Boolean actual) {
            super(id, available, requested, actual, null);
        }
    }

    /**
     * Get the state change notification flux.
     *
     * @return Flux emitting signals every time the state has changed.
     */
    Flux<Signal<State, String>> getFlux();

    /**
     * Set state.
     *
     * @return A mono that is completed when the state is actually set and verified.
     */
    Mono<Boolean> setState(boolean state);

    /**
     * Get hardware state.
     *
     * @return A mono that is completed when the state is actually obtained.
     */
    Mono<Boolean> getState();
}
