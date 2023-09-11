package net.sf.dz3r.signal;

import reactor.core.publisher.Flux;

/**
 * A reactive signal processor abstraction.
 *
 * @param <I> Input type.
 * @param <O> Output type.
 * @param <P> Signal payload type.
 *
 * @see net.sf.dz3r.controller.ProcessController
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
@FunctionalInterface
public interface SignalProcessor<I, O, P> {

    /**
     * Compute the output signal.
     *
     * @param in Input flux.
     *
     * @return Output flux. The end of this flux indicates the need for the subscriber to shut down.
     */
    Flux<Signal<O, P>> compute(Flux<Signal<I, P>> in);
}
