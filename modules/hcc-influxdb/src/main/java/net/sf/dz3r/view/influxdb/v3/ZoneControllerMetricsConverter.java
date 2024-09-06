package net.sf.dz3r.view.influxdb.v3;

import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

public class ZoneControllerMetricsConverter extends MetricsConverter<UnitControlSignal, Void> {
    public ZoneControllerMetricsConverter(String instance, String unit) {
        super(instance, unit);
    }

    @Override
    public Flux<Point> compute(Flux<Signal<UnitControlSignal, Void>> in) {
        return in.map(this::convert);
    }

    private Point convert(Signal<UnitControlSignal, Void> signal) {

        var b = Point.measurement("zoneController")
                .time(signal.timestamp().toEpochMilli(), TimeUnit.MILLISECONDS)
                .tag("instance", instance)
                .tag("unit", unit);

        var controlSignal = signal.getValue();

        if (controlSignal != null) {

            b.addField("demand", controlSignal.demand);
            b.addField("fanSpeed", controlSignal.fanSpeed);
        }

        if (signal.error() != null) {
            b.addField("error", signal.error().toString());
        }

        return b.build();
    }
}
