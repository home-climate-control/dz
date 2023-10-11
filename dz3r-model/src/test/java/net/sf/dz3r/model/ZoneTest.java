package net.sf.dz3r.model;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

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
        var signalTotalFailure = new Signal<Double, String>(Instant.now(), null, null, Signal.Status.FAILURE_TOTAL, new TimeoutException("sensor is gone"));

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
                    assertThat(s.getValue().settings.setpoint).isEqualTo(setpoint);
                    assertThat(s.getValue().callingStatus.calling).isTrue();
                    assertThat(s.payload).isEqualTo(name);
                })
                .assertNext(s -> assertThat(s.getValue().callingStatus.calling).isFalse())
                .assertNext(s -> assertThat(s.getValue().callingStatus.calling).isFalse())
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
                    assertThat(s.getValue().settings.setpoint).isEqualTo(setpoint);
                    assertThat(s.getValue().callingStatus.calling).isFalse();
                    assertThat(s.payload).isEqualTo(name);
                })
                .verifyComplete();
    }

    @Test
    @Disabled("temporary, must address before #290 is closed")
    void setpointChangeEmitsSignal() {

        var source = Flux
                .create(this::connectSetpoint)
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

        pvSink.next(15.0);
        pvSink.next(25.0);

        z.setSettingsSync(new ZoneSettings(z.getSettings(), 30.0));

        pvSink.next(35.0);

        pvSink.complete();

        // Three signals corresponding to process variable change, and one to setpoint change
        assertThat(accumulator).hasSize(4);

        // PV change
        assertThat(accumulator.get(0).getValue().callingStatus.calling).isFalse();
        assertThat(accumulator.get(1).getValue().callingStatus.calling).isTrue();

        // Setpoint change
        assertThat(accumulator.get(2).getValue().callingStatus.calling).isFalse();

        // PV change again
        assertThat(accumulator.get(3).getValue().callingStatus.calling).isTrue();

        out.dispose();
    }

    private FluxSink<Double> pvSink;


    private void connectSetpoint(FluxSink<Double> pvSink) {
        this.pvSink = pvSink;
    }
}
