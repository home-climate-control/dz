package net.sf.dz3r.view.influxdb.v3;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class HvacDeviceMetricsConverter extends MetricsConverter<HvacDeviceStatus, Void> {
    public HvacDeviceMetricsConverter(String instance, String unit) {
        super(instance, unit);
    }

    @Override
    public Flux<Point> compute(Flux<Signal<HvacDeviceStatus, Void>> in) {
        return in.map(this::convert);
    }

    private Point convert(Signal<HvacDeviceStatus, Void> signal) {

        var b = Point.measurement("hvacDevice")
                .time(signal.timestamp.toEpochMilli(), TimeUnit.MILLISECONDS)
                .tag("instance", instance)
                .tag("unit", unit);

        var status = signal.getValue();

        if (status != null) {

            b.tag("kind", status.kind.toString());
            b.addField("demand", status.requested.demand);
            b.addField("fanSpeed", status.requested.fanSpeed);
            b.addField("uptimeMillis", Optional.ofNullable(status.uptime).map(Duration::toMillis).orElse(0L));
            Optional.ofNullable(status.requested.mode).ifPresent(m -> b.tag("mode", m.toString()));
        }

        if (signal.error != null) {
            b.addField("error", signal.error.toString());
        }

        return b.build();
    }
}
