package net.sf.dz3r.instrumentation;

import com.homeclimatecontrol.hcc.signal.Signal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SensorProcessorTest {

    @Test
    void testResolution() {

        var start = Instant.now();
        var source = Flux.just(
                new Signal<Double, Void>(start, 1.00),
                new Signal<Double, Void>(start.plusMillis(1000), 1.25),
                new Signal<Double, Void>(start.plusMillis(2000), 1.75)
        );

        var sp = new SensorStatusProcessor("");
        var result = sp.compute(source).blockLast();

        assertThat(result).isNotNull();
        assertThat(result.getValue().resolution()).isEqualTo(0.25);
    }

    @Test
    void testResolutionErrorSingle() {

        var start = Instant.now();
        var source = Flux.just(
                new Signal<Double, Void>(start, null, null, Signal.Status.FAILURE_TOTAL, new Throwable("oops"))
        );

        var sp = new SensorStatusProcessor("");
        var result = sp.compute(source).blockLast();

        assertThat(result).isNotNull();
        assertThat(result.status).isEqualTo(Signal.Status.FAILURE_TOTAL);
        assertThat(result.getValue()).isNull();
    }

    @Test
    void testResolutionErrorMultiple() {

        var start = Instant.now();
        var source = Flux.just(
                new Signal<Double, Void>(start, null, null, Signal.Status.FAILURE_TOTAL, new Throwable("oops")),
                new Signal<Double, Void>(start, 1.00)
        );

        var sp = new SensorStatusProcessor("");
        var result = sp.compute(source).blockLast();

        assertThat(result).isNotNull();
        assertThat(result.status).isEqualTo(Signal.Status.OK);
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue().resolution()).isNull();
    }
}
