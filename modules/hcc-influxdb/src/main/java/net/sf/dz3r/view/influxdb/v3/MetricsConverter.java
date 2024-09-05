package net.sf.dz3r.view.influxdb.v3;

import com.homeclimatecontrol.hcc.signal.Signal;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;

/**
 * Converts the input signal into a signal suitable for the metrics collector.
 *
 * @param <I> Input signal type.
 * @param <P> Payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
abstract class MetricsConverter<I, P>  {

    public final String instance;
    public final String unit;

    protected MetricsConverter(String instance, String unit) {
        this.instance = instance;
        this.unit = unit;
    }

    public abstract Flux<Point> compute(Flux<Signal<I, P>> in);
}
