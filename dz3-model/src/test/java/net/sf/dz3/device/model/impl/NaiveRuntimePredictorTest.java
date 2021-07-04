package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.HvacSignal;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class NaiveRuntimePredictorTest {

    @Test
    void nullSignal() {

        assertThatIllegalArgumentException().isThrownBy(() -> {
            new NaiveRuntimePredictor().consume(null);
        });
    }

    @Test
    void errorSignal() {

        assertThatIllegalStateException().isThrownBy(() -> {
            new NaiveRuntimePredictor().consume(
                    new DataSample<HvacSignal>(Clock.systemUTC().millis(),
                    "unit", "signature",
                    null, new Error("oops")));
        });
    }

    @Test
    void unknownIdle() {

        var p = new NaiveRuntimePredictor();
        var sink = new Sink();
        p.addConsumer(sink);
        var now = Clock.systemUTC().instant();

        var signal1 = new DataSample<>(now.toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, 1d, false, 0L), null);
        var signal2 = new DataSample<>(now.plus(2, ChronoUnit.MINUTES).toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, 1d, false, 0L), null);

        p.consume(signal1);
        p.consume(signal2);

        assertThat(sink.consumed).hasSize(2);
        assertThat(sink.consumed.get(0).sample.left).isNull();
        assertThat(sink.consumed.get(0).sample.arrival).isNull();
        assertThat(sink.consumed.get(0).sample.plus).isFalse();
        assertThat(sink.consumed.get(1).sample.left).isNull();
        assertThat(sink.consumed.get(1).sample.arrival).isNull();
        assertThat(sink.consumed.get(1).sample.plus).isFalse();

        p.removeConsumer(sink);
    }

    @Test
    void unknownDone() {

        var p = new NaiveRuntimePredictor();
        var sink = new Sink();
        p.addConsumer(sink);
        var now = Clock.systemUTC().instant();
        var initialDemand = 1d;

        var signalJustStarted = new DataSample<>(now.toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, initialDemand, true, 0L), null);
        var signal2min = new DataSample<>(now.plus(2, ChronoUnit.MINUTES).toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, initialDemand * 1.2, false, 0), null);

        p.consume(signalJustStarted);
        p.consume(signal2min);

        assertThat(sink.consumed).hasSize(2);
        assertThat(sink.consumed.get(0).sample.left).isNull();
        assertThat(sink.consumed.get(0).sample.arrival).isNull();
        assertThat(sink.consumed.get(0).sample.plus).isFalse();
        assertThat(sink.consumed.get(1).sample.left).isNull();
        assertThat(sink.consumed.get(1).sample.arrival).isNull();
        assertThat(sink.consumed.get(1).sample.plus).isFalse();

        p.removeConsumer(sink);
    }

    @Test
    void unknownTooShort() {

        var p = new NaiveRuntimePredictor();
        var sink = new Sink();
        p.addConsumer(sink);
        var now = Clock.systemUTC().instant();
        var initialDemand = 1d;

        var signalJustStarted = new DataSample<>(now.toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, initialDemand, true, 0L), null);
        var signal30sec = new DataSample<>(now.plus(30, ChronoUnit.SECONDS).toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, initialDemand * 1.2, true, Duration.of(30, ChronoUnit.SECONDS).toMillis()), null);

        p.consume(signalJustStarted);
        p.consume(signal30sec);

        assertThat(sink.consumed).hasSize(2);
        assertThat(sink.consumed.get(0).sample.left).isNull();
        assertThat(sink.consumed.get(0).sample.arrival).isNull();
        assertThat(sink.consumed.get(0).sample.plus).isFalse();
        assertThat(sink.consumed.get(1).sample.left).isNull();
        assertThat(sink.consumed.get(1).sample.arrival).isNull();
        assertThat(sink.consumed.get(1).sample.plus).isFalse();

        p.removeConsumer(sink);
    }

    @Test
    void simple() {

        var p = new NaiveRuntimePredictor();
        var sink = new Sink();
        p.addConsumer(sink);
        var now = Clock.systemUTC().instant();
        var initialDemand = 1d;

        var signalJustStarted = new DataSample<>(now.toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, initialDemand, true, 0L), null);
        var signal2min = new DataSample<>(now.plus(2, ChronoUnit.MINUTES).toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, initialDemand / 2, true, Duration.of(2, ChronoUnit.MINUTES).toMillis()), null);

        p.consume(signalJustStarted);
        p.consume(signal2min);

        assertThat(sink.consumed).hasSize(2);
        assertThat(sink.consumed.get(0).sample.left).isNull();
        assertThat(sink.consumed.get(0).sample.arrival).isNull();
        assertThat(sink.consumed.get(0).sample.plus).isFalse();
        assertThat(sink.consumed.get(1).sample.left).isEqualTo(Duration.of(2, ChronoUnit.MINUTES));
        assertThat(sink.consumed.get(1).sample.plus).isFalse();

        p.removeConsumer(sink);
    }

    @Test
    void demandGrewWithinFirstMinute() {

        var p = new NaiveRuntimePredictor();
        var sink = new Sink();
        p.addConsumer(sink);
        var now = Clock.systemUTC().instant();

        var demand = new double[] {1d, 2d, 1d};

        var signalJustStarted = new DataSample<>(now.toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, demand[0], true, 0L), null);
        var signal30sec = new DataSample<>(now.plus(30, ChronoUnit.SECONDS).toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, demand[1], true, Duration.of(30, ChronoUnit.SECONDS).toMillis()), null);
        var signal2min = new DataSample<>(now.plus(2, ChronoUnit.MINUTES).toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, demand[2], true, Duration.of(2, ChronoUnit.MINUTES).toMillis()), null);

        p.consume(signalJustStarted);
        p.consume(signal30sec);
        p.consume(signal2min);

        assertThat(sink.consumed).hasSize(3);
        assertThat(sink.consumed.get(0).sample.left).isNull();
        assertThat(sink.consumed.get(0).sample.arrival).isNull();
        assertThat(sink.consumed.get(0).sample.plus).isFalse();
        assertThat(sink.consumed.get(1).sample.left).isNull();
        assertThat(sink.consumed.get(1).sample.arrival).isNull();
        assertThat(sink.consumed.get(1).sample.plus).isFalse();
        assertThat(sink.consumed.get(2).sample.left).isEqualTo(Duration.of(2, ChronoUnit.MINUTES));
        assertThat(sink.consumed.get(2).sample.plus).isFalse();

        p.removeConsumer(sink);
    }

    @Test
    void insufficient() {

        var p = new NaiveRuntimePredictor();
        var sink = new Sink();
        p.addConsumer(sink);
        var now = Clock.systemUTC().instant();
        var initialDemand = 1d;

        var signalJustStarted = new DataSample<>(now.toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, initialDemand, true, 0L), null);
        var signal2min = new DataSample<>(now.plus(2, ChronoUnit.MINUTES).toEpochMilli(),
                "unit", "signature",
                new HvacSignal(HvacMode.COOLING, initialDemand * 1.2, true, Duration.of(2, ChronoUnit.MINUTES).toMillis()), null);

        p.consume(signalJustStarted);
        p.consume(signal2min);

        assertThat(sink.consumed).hasSize(2);
        assertThat(sink.consumed.get(0).sample.left).isNull();
        assertThat(sink.consumed.get(0).sample.arrival).isNull();
        assertThat(sink.consumed.get(0).sample.plus).isFalse();
        assertThat(sink.consumed.get(1).sample.left).isNull();
        assertThat(sink.consumed.get(1).sample.arrival).isNull();
        assertThat(sink.consumed.get(1).sample.plus).isTrue();

        p.removeConsumer(sink);
    }

    private class Sink implements DataSink<UnitRuntimePredictionSignal> {

        public final List<DataSample<UnitRuntimePredictionSignal>> consumed = new ArrayList<>();

        @Override
        public void consume(DataSample<UnitRuntimePredictionSignal> signal) {
            consumed.add(signal);
        }
    }
}
