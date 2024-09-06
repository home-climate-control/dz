package net.sf.dz3r.signal.filter;

import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import reactor.core.publisher.Flux;

/**
 * Reactive converting filter.
 *
 * @param <P> Payload type.
 *
 * @see AnalogConverterLM34
 * @see AnalogConverterTMP36
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public class ConvertingFilter<P> implements SignalProcessor<Double, Double, P> {

    public final AnalogConverter converter;

    public ConvertingFilter(AnalogConverter converter) {
        this.converter = converter;
    }

    @Override
    public Flux<Signal<Double, P>> compute(Flux<Signal<Double, P>> in) {
        return in.map(signal -> new Signal<>(signal.timestamp(), converter.convert(signal.getValue()), signal.payload(), signal.status(), signal.error()));
    }
}
