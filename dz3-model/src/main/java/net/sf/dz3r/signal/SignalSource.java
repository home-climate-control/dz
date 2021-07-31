package net.sf.dz3r.signal;

import net.sf.dz3r.device.Addressable;
import reactor.core.publisher.Flux;

/**
 * Base interface for all signal sources.
 *
 * @param <A> Signal address type.
 * @param <S> Signal source reference type.
 * @param <V> Signal value type.
 * @param <T> Signal source address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public interface SignalSource<A extends Comparable<A>, S extends Addressable<A>, V, T extends Comparable<T>> extends Addressable<T> {

    /**
     * Get the signal flux.
     *
     * This flux will only terminate if instructed to do so. Problems with the source will be reflected in
     * {@link Signal#getStatus() signal status}.
     *
     * @return Signal flux.
     */
    Flux<Signal<A, S, V>> getFlux();
}
