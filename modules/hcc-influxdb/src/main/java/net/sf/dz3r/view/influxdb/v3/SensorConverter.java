package net.sf.dz3r.view.influxdb.v3;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.dto.Point;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

public class SensorConverter {

    private final Logger logger = LogManager.getLogger();

    private final String instance;
    private final String name;

    public SensorConverter(String instance, String name) {
        this.instance = instance;
        this.name = name;
    }

    public Flux<Point> compute(Flux<Signal<Double, Void>> in) {
        return in.map(this::convert)
                .doOnNext(s -> logger.trace("compute: {}", s));
    }

    private Point convert(Signal<Double, Void> signal) {

        var b = Point.measurement("sensor")
                .time(signal.timestamp.toEpochMilli(), TimeUnit.MILLISECONDS)
                .tag("instance", instance)
                .tag("name", name);

        if (signal.getValue() != null) {
            b.addField("sample", signal.getValue());
        }

        if (signal.error != null) {
            b.addField("error", signal.error.toString());
        }

        return b.build();
    }
}
