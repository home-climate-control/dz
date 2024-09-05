package net.sf.dz3r.signal;

import com.homeclimatecontrol.hcc.signal.Signal;
import reactor.core.publisher.Flux;

/**
 * Base interface for all signal sources.
 *
 * @param <A> Address type.
 * @param <T> Signal value type.
 * @param <P> Extra payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public interface SignalSource<A extends Comparable<A>, T, P> {

    /**
     * Get the signal flux.
     *
     * This flux will only terminate if instructed to do so. Problems with the source will be reflected in
     * {@link Signal#status signal status}.
     *
     * @return Signal flux.
     */
    Flux<Signal<T, P>> getFlux(A address);
}
