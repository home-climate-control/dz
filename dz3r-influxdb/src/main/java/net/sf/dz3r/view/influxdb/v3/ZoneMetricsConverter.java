package net.sf.dz3r.view.influxdb.v3;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class ZoneMetricsConverter extends MetricsConverter<ZoneStatus, String> {

    protected ZoneMetricsConverter(String instance, String unit) {
        super(instance, unit);
    }

    @Override
    public Flux<Point> compute(Flux<Signal<ZoneStatus, String>> in) {
        return in.flatMap(this::convert);
    }

    private Flux<Point> convert(Signal<ZoneStatus, String> signal) {

        return Flux.concat(convertZone(signal), convertEconomizer(signal));
    }

    private Mono<Point> convertZone(Signal<ZoneStatus, String> signal) {

        var b = createBuilder(signal, "zone");

        var status = signal.getValue();

        if (status != null) {

            b.addField("enabled", status.settings.enabled);
            b.addField("setpoint", status.settings.setpoint);
            b.addField("voting", status.settings.voting);
            b.addField("hold", status.settings.hold);
            b.addField("dumpPriority", status.settings.dumpPriority);

            b.addField("calling", status.callingStatus.calling);
            b.addField("demand", status.callingStatus.demand);
        }

        if (signal.error != null) {
            b.addField("error", signal.error.toString());
        }

        return Mono.just(b.build());
    }
    private Mono<Point> convertEconomizer(Signal<ZoneStatus, String> signal) {

        var status = signal.getValue();

        if (status == null || status.economizerStatus == null) {

            // Not returning an error here, will have to live and see if this is a problem
            // (it's already emitted by the zone itself)
            return Mono.empty();
        }

        var b = createBuilder(signal, "economizer");

        var economizerStatus = status.economizerStatus;

        b.addField("enabled", economizerStatus.settings.enabled);
        b.addField("delta", economizerStatus.settings.changeoverDelta);
        b.addField("target", economizerStatus.settings.targetTemperature);

        b.addField("calling", economizerStatus.callingStatus.calling);
        b.addField("demand", economizerStatus.callingStatus.demand);

        // May not be ready yet at startup time
        if (economizerStatus.ambient != null) {
            b.addField("ambient", economizerStatus.ambient.getValue());
        }

        return Mono.just(b.build());
    }

    private Point.Builder createBuilder(Signal<ZoneStatus, String> signal, String measurement) {

        return Point.measurement(measurement)
                .time(signal.timestamp.toEpochMilli(), TimeUnit.MILLISECONDS)
                .tag("instance", instance)
                .tag("unit", unit)
                .tag("name", signal.payload);
    }
}
