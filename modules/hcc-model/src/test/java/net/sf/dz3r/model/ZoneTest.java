package net.sf.dz3r.model;

import com.homeclimatecontrol.hcc.model.ZoneSettings;
import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static com.homeclimatecontrol.hcc.signal.Signal.Status.FAILURE_TOTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZoneTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void comparable() {

        var ts1 = mock(Thermostat.class);
        var ts2 = mock(Thermostat.class);

        when(ts1.getAddress()).thenReturn("a");
        when(ts2.getAddress()).thenReturn("b");

        // Can't mock ZoneSettings, direct member access
        var zs1 = new ZoneSettings(20.0);
        var zs2 = new ZoneSettings(21.0);

        var z1 = new Zone(ts1, zs1);
        var z2 = new Zone(ts2, zs2);

        var set = new TreeSet<Zone>();

        set.add(z1);
        set.add(z2);

        assertThat(set).hasSize(2);
    }

    @Test
    void enabled() throws Exception {

        var setpoint = 20.0;
        var signalOK = new Signal<Double, String>(Instant.now(), 30.0);
        var signalPartialFailure = new Signal<Double, String>(Instant.now(), 10.0, null, Signal.Status.FAILURE_PARTIAL, new TimeoutException("stale sensor"));
        var signalTotalFailure = new Signal<Double, String>(Instant.now(), null, null, FAILURE_TOTAL, new TimeoutException("sensor is gone"));

        var sequence = Flux.just(
                signalOK,
                signalPartialFailure,
                signalTotalFailure
        );

        var name = UUID.randomUUID().toString();
        var ts = new Thermostat(name, setpoint, 1, 0, 0, 1);
        var z = new Zone(ts, new ZoneSettings(ts.getSetpoint()));

        var out = z
                .compute(sequence)
                .doOnNext(e -> logger.debug("zone/ON: {}", e));

        z.close();

        StepVerifier
                .create(out)
                .assertNext(s -> {
                    assertThat(s.getValue().settings().setpoint()).isEqualTo(setpoint);
                    assertThat(s.getValue().callingStatus().calling()).isTrue();
                    assertThat(s.payload()).isEqualTo(name);
                })
                .assertNext(s -> assertThat(s.getValue().callingStatus().calling()).isFalse())
                .assertNext(s -> assertThat(s.getValue().callingStatus().calling()).isFalse())
                .verifyComplete();
    }

    @Test
    void disabled() throws Exception {

        var setpoint = 20.0;
        var signalOK = new Signal<Double, String>(Instant.now(), 30.0);
        var sequence = Flux.just(signalOK);
        var name = UUID.randomUUID().toString();
        var ts = new Thermostat(name, setpoint, 1, 0, 0, 1);
        var settings = new ZoneSettings(ts.getSetpoint());
        var z = new Zone(ts, settings);

        z.setSettingsSync(new ZoneSettings(settings, false));

        var out = z
                .compute(sequence)
                .doOnNext(e -> logger.debug("zone/{}}: {}", name, e));

        z.close();

        // The thermostat is calling, but the zone has shut it off
        StepVerifier
                .create(out)
                .assertNext(s -> {
                    assertThat(s.getValue().settings().setpoint()).isEqualTo(setpoint);
                    assertThat(s.getValue().callingStatus().calling()).isFalse();
                    assertThat(s.payload()).isEqualTo(name);
                })
                .verifyComplete();
    }

    @Test
    void setpointChangeEmitsSignal() {

        var pvWrapper = new SinkWrapper<Double>();
        var source = Flux
                .create(pvWrapper::connect)
                .map(v -> new Signal<Double, String>(Instant.now(), v));

        var setpoint = 20.0;
        var name = UUID.randomUUID().toString();
        var ts = new Thermostat(name, setpoint, 1, 0, 0, 1);
        var z = new Zone(ts, new ZoneSettings(ts.getSetpoint()));

        var accumulator = new ArrayList<Signal<ZoneStatus, String>>();
        var out = z
                .compute(source)
                .log()
                .subscribe(accumulator::add);

        pvWrapper.sink.next(15.0);
        pvWrapper.sink.next(25.0);

        z.setSettingsSync(new ZoneSettings(z.getSettings(), 30.0));

        pvWrapper.sink.next(35.0);

        pvWrapper.sink.complete();

        // Three signals corresponding to process variable change, and one to setpoint change
        assertThat(accumulator).hasSize(5);

        // PV change
        assertThat(accumulator.get(0).getValue().callingStatus().calling()).isFalse();
        assertThat(accumulator.get(1).getValue().callingStatus().calling()).isTrue();

        // Setpoint change; one replayed by AbstractProcessController, another replayed by Zone
        assertThat(accumulator.get(2).getValue().callingStatus().calling()).isFalse();
        assertThat(accumulator.get(3).getValue().callingStatus().calling()).isFalse();

        // PV change again
        assertThat(accumulator.get(4).getValue().callingStatus().calling()).isTrue();

        out.dispose();
    }

    /**
     * Make sure the error signal is replayed when setpoint is changed.
     *
     * See <a href="https://github.com/home-climate-control/dz/issues/333">#333</a>.
     */
    @Test
    void errorSignalReplayed() {

        var pvWrapper = new SinkWrapper<Signal<Double, String>>();
        var source = Flux
                .create(pvWrapper::connect);

        var setpoint = 30.0;
        var name = UUID.randomUUID().toString();
        var ts = new Thermostat(name, setpoint, 1, 0, 0, 1);
        var z = new Zone(ts, new ZoneSettings(ts.getSetpoint()));

        var accumulator = new ArrayList<Signal<ZoneStatus, String>>();
        var out = z
                .compute(source)
                .log()
                .subscribe(accumulator::add);

        var start = Instant.now();

        // A valid signal not causing the zone to call
        pvWrapper.sink.next(new Signal<>(start, 25.0));

        // Error signal 30 seconds later
        pvWrapper.sink.next(new Signal<>(start.plus(Duration.ofSeconds(30)), null, null, FAILURE_TOTAL, new IllegalStateException("timeout")));

        // Setpoint set so that the last known signal would have caused it to start calling
        // With #333, this DOES cause it to start calling - but the cause is not here
        z.setSettingsSync(new ZoneSettings(z.getSettings(), 20.0));

        pvWrapper.sink.complete();

        // Three signals corresponding to process variable change, and one to setpoint change
        assertThat(accumulator).hasSize(4);

        // PV change
        assertThat(accumulator.get(0).getValue().callingStatus().calling()).isFalse();
        assertThat(accumulator.get(0).status()).isEqualTo(Signal.Status.OK);

        // Error signal received
        assertThat(accumulator.get(1).getValue().callingStatus().calling()).isFalse();
        assertThat(accumulator.get(1).status()).isEqualTo(Signal.Status.FAILURE_TOTAL);

        // Setpoint change; one replayed by AbstractProcessController, another replayed by Zone
        assertThat(accumulator.get(2).getValue().callingStatus().calling()).isFalse();
        assertThat(accumulator.get(2).status()).isEqualTo(Signal.Status.FAILURE_TOTAL);
        assertThat(accumulator.get(3).getValue().callingStatus().calling()).isFalse();
        assertThat(accumulator.get(2).status()).isEqualTo(Signal.Status.FAILURE_TOTAL);

        out.dispose();
    }

    private static class SinkWrapper<T> {
        FluxSink<T> sink;
        void connect(FluxSink<T> sink) {
            this.sink = sink;
        }
    }
}
