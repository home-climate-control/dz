package net.sf.dz3r.signal;

import net.sf.dz3r.device.Addressable;
import reactor.core.publisher.Flux;

/**
 * Base interface for all signal sources.
 *
 * @param <S> Signal source reference type.
 * @param <V> Signal value type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public interface SignalSource<S extends Addressable, V> extends Addressable {

    /**
     * Get the signal flux.
     *
     * This flux will only terminate if instructed to do so. Problems with the source will be reflected in
     * {@link Signal#getStatus() signal status}.
     *
     * @return Signal flux.
     */
    Flux<Signal<S, V>> getFlux();
}
