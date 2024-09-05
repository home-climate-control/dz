package net.sf.dz3r.view.influxdb.v3;

import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.HvacCommand;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class UnitControllerMetricsConverter extends MetricsConverter<HvacCommand, Void>{
    public UnitControllerMetricsConverter(String instance, String unit) {
        super(instance, unit);
    }

    @Override
    public Flux<Point> compute(Flux<Signal<HvacCommand, Void>> in) {
        return in.map(this::convert);
    }

    private Point convert(Signal<HvacCommand, Void> signal) {

        var b = Point.measurement("unitController")
                .time(signal.timestamp.toEpochMilli(), TimeUnit.MILLISECONDS)
                .tag("instance", instance)
                .tag("unit", unit);

        var hvacCommand = signal.getValue();

        if (hvacCommand != null) {

            b.addField("demand", hvacCommand.demand());
            b.addField("fanSpeed", hvacCommand.fanSpeed());
            Optional.ofNullable(hvacCommand.mode()).ifPresent(m -> b.tag("mode", m.toString()));
        }

        if (signal.error != null) {
            b.addField("error", signal.error.toString());
        }

        return b.build();
    }
}
