package net.sf.dz3r.signal;

import reactor.core.publisher.Flux;

/**
 * Base interface for all signal sources.
 *
 * @param <T> Signal value type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public interface SignalSource<A extends Comparable<A>, T> {

    /**
     * Get the signal flux.
     *
     * This flux will only terminate if instructed to do so. Problems with the source will be reflected in
     * {@link Signal#getStatus() signal status}.
     *
     * @return Signal flux.
     */
    Flux<Signal<T>> getFlux();
}
