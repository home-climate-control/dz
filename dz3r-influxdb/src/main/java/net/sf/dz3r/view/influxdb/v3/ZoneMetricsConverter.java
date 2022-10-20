package net.sf.dz3r.view.influxdb.v3;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

public class ZoneMetricsConverter extends MetricsConverter<ZoneStatus, String> {

    protected ZoneMetricsConverter(String instance, String unit) {
        super(instance, unit);
    }

    @Override
    public Flux<Point> compute(Flux<Signal<ZoneStatus, String>> in) {
        return in.map(this::convert);
    }

    private Point convert(Signal<ZoneStatus, String> signal) {

        var b = Point.measurement("zone")
                .time(signal.timestamp.toEpochMilli(), TimeUnit.MILLISECONDS)
                .tag("instance", instance)
                .tag("unit", unit)
                .tag("name", signal.payload);

        var status = signal.getValue();

        if (status != null) {

            b.addField("enabled", status.settings.enabled);
            b.addField("setpoint", status.settings.setpoint);
            b.addField("voting", status.settings.voting);
            b.addField("hold", status.settings.hold);
            b.addField("dumpPriority", status.settings.dumpPriority);

            b.addField("calling", status.thermostatStatus.calling);
            b.addField("demand", status.thermostatStatus.demand);
        }

        if (signal.error != null) {
            b.addField("error", signal.error.toString());
        }

        return b.build();
    }
}
