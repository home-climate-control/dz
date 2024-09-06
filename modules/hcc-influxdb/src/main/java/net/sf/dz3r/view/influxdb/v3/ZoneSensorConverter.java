package net.sf.dz3r.view.influxdb.v3;

import com.homeclimatecontrol.hcc.signal.Signal;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

public class ZoneSensorConverter extends MetricsConverter<Double, Void>{

    public final String zoneName;

    protected ZoneSensorConverter(String instance, String unit, String zoneName) {
        super(instance, unit);
        this.zoneName = zoneName;
    }

    @Override
    public Flux<Point> compute(Flux<Signal<Double, Void>> in) {
        return in.map(this::convert);
    }

    private Point convert(Signal<Double, Void> signal) {

        var b = Point.measurement("zone")
                .time(signal.timestamp().toEpochMilli(), TimeUnit.MILLISECONDS)
                .tag("instance", instance)
                .tag("unit", unit)
                .tag("name", zoneName);

        if (signal.getValue() != null) {
            b.addField("temperature", signal.getValue());
        }

        if (signal.error() != null) {
            b.addField("error", signal.error().toString());
        }

        return b.build();
    }
}
