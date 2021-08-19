package net.sf.dz3r.model;

import net.sf.dz3r.controller.pid.SimplePidController;
import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See {@code net.sf.dz3.device.model.impl.SimpleZoneControllerTest}
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
class ZoneControllerTest {

    private List<Double> source = List.of(
            20.0,
            20.5,
            21.0,
            20.5,
            20.0,
            19.5,
            19.0
    );

    /**
     * Test the P controller from {@link #testOneZone()}.
     */
    @Test
    void testOneP() {

        var offset = new AtomicInteger();
        var sequence = Flux
                .fromIterable(source)
                .map(e -> new Signal<Double, Void>(Instant.now().plus(offset.getAndIncrement(), ChronoUnit.SECONDS), e));
        var controller = new SimplePidController<Void>("simple20",  20.0, 1.0, 0, 0, 0);

        var stage1 = controller.compute(sequence);

        StepVerifier
                .create(stage1)
                .assertNext(s -> assertThat(s.getValue().signal).isZero()) // 20.0
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(0.5)) // 20.5
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(1.0)) // 21.0
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(0.5)) // 20.5
                .assertNext(s -> assertThat(s.getValue().signal).isZero()) // 20.0
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(-0.5)) // 19.5
                .assertNext(s -> assertThat(s.getValue().signal).isEqualTo(-1.0)) // 19.0
                .verifyComplete();
    }

    /**
     * Test the thermostat from {@link #testOneZone()}.
     */
    @Test
    void testOneThermostat() {

        var offset = new AtomicInteger();
        var sequence = Flux
                .fromIterable(source)
                .map(e -> new Signal<Double, Void>(Instant.now().plus(offset.getAndIncrement(), ChronoUnit.SECONDS), e));
        var ts = new Thermostat("ts", 20, 1, 0, 0, 1);

        var stage1 = ts.compute(sequence);

        StepVerifier
                .create(stage1)
                .assertNext(s -> {
                    // 20.0 - initially, off
                    assertThat(s.getValue().signal.calling).isFalse();
                    assertThat(s.getValue().signal.demand).isEqualTo(1.0);
                })
                .assertNext(s -> {
                    // 20.5 - still off
                    assertThat(s.getValue().signal.calling).isFalse();
                    assertThat(s.getValue().signal.demand).isEqualTo(1.5);
                })
                .assertNext(s -> {
                    // 21.0 - turning on
                    assertThat(s.getValue().signal.calling).isTrue();
                    assertThat(s.getValue().signal.demand).isEqualTo(2.0);
                })
                .assertNext(s -> {
                    // 20.5 - still on
                    assertThat(s.getValue().signal.calling).isTrue();
                    assertThat(s.getValue().signal.demand).isEqualTo(1.5);
                })
                .assertNext(s -> {
                    // 20.0 - still on
                    assertThat(s.getValue().signal.calling).isTrue();
                    assertThat(s.getValue().signal.demand).isEqualTo(1.0);
                })
                .assertNext(s -> {
                    // 19.5 - still on
                    assertThat(s.getValue().signal.calling).isTrue();
                    assertThat(s.getValue().signal.demand).isEqualTo(0.5);
                })
                .assertNext(s -> {
                    // 19.0 - turning off now
                    assertThat(s.getValue().signal.calling).isFalse();
                    assertThat(s.getValue().signal.demand).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    /**
     * Simplest possible configuration: one thermostat.
     */
    @Test
    void testOneZone() {

        var offset = new AtomicInteger();
        var sequence = Flux
                .fromIterable(source)
                .map(e -> new Signal<Double, String>(Instant.now().plus(offset.getAndIncrement(), ChronoUnit.SECONDS), e));

        var ts = new Thermostat("ts", 20, 1, 0, 0, 1);
        var z = new Zone(ts, new ZoneSettings(ts.getSetpoint()));
        var zc = new ZoneController();

        var stage1 = z.compute(sequence);
        var stage2 = zc.compute(stage1).log();

        // DZ-reactive sequence
        StepVerifier
                .create(stage2)
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(0.0)) // 20.0
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(0.0)) // 20.5
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(2.0)) // 21.0
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(1.5)) // 20.5
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(1.0)) // 20.0
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(0.5)) // 19.5
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(0.0)) // 19.0
                .verifyComplete();
    }

    /**
     * Test the <a href="https://code.google.com/archive/p/diy-zoning/issues/1">"Cold Start" bug</a>.
     *
     * The zone controller should stay off without exceptions when the first ever signal
     * doesn't indicate calling.
     */
    @Test
    void testColdStartNotCalling() {

        var ts1 = new Thermostat("ts20", 20, 1, 0, 0, 1);
        var z1 = new Zone(ts1, new ZoneSettings(ts1.getSetpoint()));

        var ts2 = new Thermostat("ts25", 25, 1, 0, 0, 1);
        var z2 = new Zone(ts2, new ZoneSettings(ts2.getSetpoint()));

        var zc = new ZoneController();

        var sequence = Flux
                .just(new Signal<Double, String>(Instant.now(), 20.0));

        var flux1 = z1.compute(sequence);
        var flux2 = z2.compute(sequence);

        // Note merge(), order is irrelevant, zero demand
        var fluxZ = zc.compute(Flux.merge(flux1, flux2));

        StepVerifier
                .create(fluxZ)
                .assertNext(s -> assertThat(s.getValue().demand).isZero())
                .assertNext(s -> assertThat(s.getValue().demand).isZero())
                .verifyComplete();
    }

    /**
     * Test the <a href="https://code.google.com/archive/p/diy-zoning/issues/1">"Cold Start" bug</a>.
     *
     * The zone controller should switch on when the first ever thermostat signal
     * indicates calling.
     */
    @Test
    void testColdStartCalling() {

        var ts1 = new Thermostat("ts20", 20, 1, 0, 0, 1);
        var z1 = new Zone(ts1, new ZoneSettings(ts1.getSetpoint()));

        var ts2 = new Thermostat("ts25", 25, 1, 0, 0, 1);
        var z2 = new Zone(ts2, new ZoneSettings(ts2.getSetpoint()));

        var zc = new ZoneController();

        var sequence = Flux
                .just(new Signal<Double, String>(Instant.now(), 30.0));

        var flux1 = z1.compute(sequence);
        var flux2 = z2.compute(sequence);

        // Note concat(), order is important for StepVerifier
        var fluxZ = zc.compute(Flux.concat(flux1, flux2));

        StepVerifier
                .create(fluxZ)
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(11.0))
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(17.0))
                .verifyComplete();
    }
}