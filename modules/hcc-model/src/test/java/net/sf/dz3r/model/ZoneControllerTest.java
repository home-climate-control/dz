package net.sf.dz3r.model;

import com.homeclimatecontrol.hcc.model.ZoneSettings;
import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;
import net.sf.dz3r.controller.pid.SimplePidController;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.homeclimatecontrol.hcc.TimeTool.atDayStartUTC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2025
 */
class ZoneControllerTest {

    private final Logger logger = LogManager.getLogger();

    private final List<Double> source = List.of(
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
                .map(e -> new Signal<Double, Void>(atDayStartUTC().plus(offset.getAndIncrement(), ChronoUnit.SECONDS), e));
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
                .map(e -> new Signal<Double, Void>(atDayStartUTC().plus(offset.getAndIncrement(), ChronoUnit.SECONDS), e));
        var ts = new Thermostat("ts", 20.0, 1, 0, 0, 1);

        var stage1 = ts.compute(sequence);

        StepVerifier
                .create(stage1)
                .assertNext(s -> {
                    // 20.0 - initially, off
                    assertThat(s.getValue().signal.calling()).isFalse();
                    assertThat(s.getValue().signal.demand()).isEqualTo(1.0);
                })
                .assertNext(s -> {
                    // 20.5 - still off
                    assertThat(s.getValue().signal.calling()).isFalse();
                    assertThat(s.getValue().signal.demand()).isEqualTo(1.5);
                })
                .assertNext(s -> {
                    // 21.0 - turning on
                    assertThat(s.getValue().signal.calling()).isTrue();
                    assertThat(s.getValue().signal.demand()).isEqualTo(2.0);
                })
                .assertNext(s -> {
                    // 20.5 - still on
                    assertThat(s.getValue().signal.calling()).isTrue();
                    assertThat(s.getValue().signal.demand()).isEqualTo(1.5);
                })
                .assertNext(s -> {
                    // 20.0 - still on
                    assertThat(s.getValue().signal.calling()).isTrue();
                    assertThat(s.getValue().signal.demand()).isEqualTo(1.0);
                })
                .assertNext(s -> {
                    // 19.5 - still on
                    assertThat(s.getValue().signal.calling()).isTrue();
                    assertThat(s.getValue().signal.demand()).isEqualTo(0.5);
                })
                .assertNext(s -> {
                    // 19.0 - turning off now
                    assertThat(s.getValue().signal.calling()).isFalse();
                    assertThat(s.getValue().signal.demand()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    /**
     * Simplest possible configuration: one thermostat.
     */
    @Test
    void testOneZone() throws Exception {

        var offset = new AtomicInteger();
        var sequence = Flux
                .fromIterable(source)
                .map(e -> new Signal<Double, String>(atDayStartUTC().plus(offset.getAndIncrement(), ChronoUnit.SECONDS), e));

        var ts = new Thermostat("ts", 20.0, 1, 0, 0, 1);
        var z = new Zone(ts, new ZoneSettings(ts.getSetpoint()));
        var zc = new ZoneController(Set.of(z));

        var stage1 = z.compute(sequence);
        var stage2 = zc.compute(stage1).log();

        z.close();

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
    void testColdStartNotCalling() throws Exception {

        var ts1 = new Thermostat("ts20", 20.0, 1, 0, 0, 1);
        var z1 = new Zone(ts1, new ZoneSettings(ts1.getSetpoint()));

        var ts2 = new Thermostat("ts25", 25.0, 1, 0, 0, 1);
        var z2 = new Zone(ts2, new ZoneSettings(ts2.getSetpoint()));

        var zc = new ZoneController(Set.of(z1, z2));

        var sequence = Flux
                .just(new Signal<Double, String>(atDayStartUTC(), 20.0));

        var flux1 = z1.compute(sequence);
        var flux2 = z2.compute(sequence);

        // Note merge(), order is irrelevant, zero demand
        var fluxZ = zc.compute(Flux.merge(flux1, flux2));

        z1.close();
        z2.close();

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
    void testColdStartCalling() throws Exception {

        var ts1 = new Thermostat("ts20", 20.0, 1, 0, 0, 1);
        var z1 = new Zone(ts1, new ZoneSettings(ts1.getSetpoint()));

        var ts2 = new Thermostat("ts25", 25.0, 1, 0, 0, 1);
        var z2 = new Zone(ts2, new ZoneSettings(ts2.getSetpoint()));

        var zc = new ZoneController(Set.of(z1, z2));

        var sequence = Flux
                .just(new Signal<Double, String>(atDayStartUTC(), 30.0));

        var flux1 = z1.compute(sequence);
        var flux2 = z2.compute(sequence);

        // Note concat(), order is important for StepVerifier
        var fluxZ = zc.compute(Flux.concat(flux1, flux2));

        z1.close();
        z2.close();

        StepVerifier
                .create(fluxZ)
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(11.0))
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(17.0))
                .verifyComplete();
    }

    /**
     * Make sure non-voting zones don't start the HVAC.
     */
    @Test
    void nonVoting() throws Exception {

        var setpoint1 = 20.0;
        var setpoint2 = 25.0;

        var s1 = new ZoneSettings(true, setpoint1, false, false, 0, null);
        var s2 = new ZoneSettings(true, setpoint2, true, false, 0, null);

        var ts1 = new Thermostat("ts20", setpoint1, 1, 0, 0, 1);
        var z1 = new Zone(ts1, s1);

        var ts2 = new Thermostat("ts25", setpoint2, 1, 0, 0, 1);
        var z2 = new Zone(ts2, s2);

        var zc = new ZoneController(Set.of(z1, z2));

        // This should bump z2 to calling, but z1 should stay off
        var sequence = Flux
                .just(new Signal<Double, String>(atDayStartUTC(), 23.0));

        var flux1 = z1.compute(sequence);
        var flux2 = z2.compute(sequence);

        // Note concat(), order is important for StepVerifier
        var fluxZ = zc.compute(Flux.concat(flux1, flux2));

        z1.close();
        z2.close();

        StepVerifier
                .create(fluxZ)
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(0.0))
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(0.0))
                .verifyComplete();
    }

    /**
     * Make sure the <a href="https://github.com/home-climate-control/dz/issues/195">last enabled zone's voting status is ignored</a>
     * for the case when it is the last enabled zone of many.
     */
    @Test
    void lastZoneOfManyNonVoting() throws Exception {

        var setpoint1 = 20.0;
        var setpoint2 = 25.0;

        // Enabled, but not voting
        var s1 = new ZoneSettings(true, setpoint1, false, false, 0, null);

        // Disabled, but voting
        var s2 = new ZoneSettings(false, setpoint1, true, false, 0, null);

        var ts1 = new Thermostat("ts20", setpoint1, 1, 0, 0, 1);
        var z1 = new Zone(ts1, s1);

        var ts2 = new Thermostat("ts25", setpoint2, 1, 0, 0, 1);
        var z2 = new Zone(ts2, s2);

        var zc = new ZoneController(Set.of(z1, z2));

        var sequence = Flux
                .just(new Signal<Double, String>(atDayStartUTC(), 23.0));

        var flux1 = z1.compute(sequence);
        var flux2 = z2.compute(sequence);

        // Note concat(), order is important for StepVerifier
        var fluxZ = zc.compute(Flux.concat(flux1, flux2));

        z1.close();
        z2.close();

        StepVerifier
                .create(fluxZ)
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(4.0))
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(4.0))
                .verifyComplete();
    }

    /**
     * Make sure the <a href="https://github.com/home-climate-control/dz/issues/195">last enabled zone's voting status is ignored</a>,
     * for the case when it is the only zone configured for the zone controller.
     */
    @Test
    void onlyZoneNonVoting() throws Exception {

        var setpoint1 = 20.0;

        // Enabled, but not voting
        var s1 = new ZoneSettings(true, setpoint1, false, false, 0, null);

        var ts1 = new Thermostat("ts20", setpoint1, 1, 0, 0, 1);
        var z1 = new Zone(ts1, s1);

        var zc = new ZoneController(Set.of(z1));

        var sequence = Flux
                .just(new Signal<Double, String>(atDayStartUTC(), 23.0));

        var flux1 = z1.compute(sequence);
        var fluxZ = zc.compute(flux1);

        z1.close();

        StepVerifier
                .create(fluxZ)
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(4.0))
                .verifyComplete();
    }
    /**
     * Make sure disabled thermostats don't start the HVAC unit.
     */
    @Test
    void disabled() throws Exception {

        var setpoint1 = 20.0;

        // Disabled
        var s1 = new ZoneSettings(false, setpoint1, true, false, 0, null);

        var ts1 = new Thermostat("ts20", setpoint1, 1, 0, 0, 1);
        var z1 = new Zone(ts1, s1);

        var zc = new ZoneController(Set.of(z1));

        var sequence = Flux
                .just(new Signal<Double, String>(atDayStartUTC(), 23.0));

        var flux1 = z1.compute(sequence);
        var fluxZ = zc.compute(flux1);

        z1.close();

        StepVerifier
                .create(fluxZ)
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(0.0))
                .verifyComplete();
    }

    /**
     * Make sure the zone controller with a single zone shuts of when the zone sends an error signal.
     */
    @Test
    void errorSignalSingleZone() throws Exception {

        var ts = new Thermostat("ts", 20.0, 1, 0, 0, 1);
        var z = new Zone(ts, new ZoneSettings(ts.getSetpoint()));

        var zc = new ZoneController(Set.of(z));
        var start = atDayStartUTC();

        var sequence = Flux
                .just(
                        new Signal<Double, String>(start, 30.0),
                        new Signal<Double, String>(start.plus(1, ChronoUnit.SECONDS), null, null, Signal.Status.FAILURE_TOTAL, new IllegalStateException("test")),
                        new Signal<Double, String>(start.plus(2, ChronoUnit.SECONDS), 30.0)
                        );

        var fluxSignal = z.compute(sequence);
        var fluxZone = zc.compute(fluxSignal);

        z.close();

        // Error signal from the only zone means we need to shut the unit off
        StepVerifier
                .create(fluxZone)
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(11.0))
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(0.0))
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(11.0))
                .verifyComplete();
    }

    /**
     * Make sure the zone controller adjusts demand, treating error zones as non-existent.
     */
    @Test
    void errorSignalOneInMultiZone() throws Exception {

        var ts1 = new Thermostat("ts20", 20.0, 1, 0, 0, 1);
        var z1 = new Zone(ts1, new ZoneSettings(ts1.getSetpoint()));

        var ts2 = new Thermostat("ts25", 25.0, 1, 0, 0, 1);
        var z2 = new Zone(ts2, new ZoneSettings(ts2.getSetpoint()));

        var zc = new ZoneController(Set.of(z1, z2));
        var start = atDayStartUTC();

        var sequence1 = Flux
                .just(
                        new Signal<Double, String>(start, 30.0),
                        new Signal<Double, String>(start.plus(6, ChronoUnit.SECONDS), 30.0)
                );
        var sequence2 = Flux
                .just(
                        new Signal<Double, String>(start.plus(2, ChronoUnit.SECONDS), 30.0),
                        new Signal<Double, String>(start.plus(4, ChronoUnit.SECONDS), null, null, Signal.Status.FAILURE_TOTAL, new IllegalStateException("test")),
                        new Signal<Double, String>(start.plus(8, ChronoUnit.SECONDS), 30.0)
                );

        var flux1 = z1.compute(sequence1);
        var flux2 = z2.compute(sequence2);

        var fluxZ = zc
                .compute(Flux
                        .concat(flux1, flux2)
                        // Line signals up in time
                        .sort((a, b) -> a.timestamp().compareTo(b.timestamp()))
                );

        z1.close();
        z2.close();

        // Error signal from just one zone means we just adjust the demand accordingly
        StepVerifier
                .create(fluxZ)
                // @0 - initial demand from z1
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(11.0))
                // @+2 - z2 adds its demand
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(17.0))
                // @+4 - z2 goes offline, its demand is ignored
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(11.0))
                // @+6 z1 confirms demand, no summary change
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(11.0))
                // @+8 z2 goes online, its demand contributes again
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(17.0))
                .verifyComplete();
    }

    /**
     * Make sure the zone controller handles alien zone incoming signals as expected.
     */
    @Test
    void alienZone() throws Exception {

        var ts1 = new Thermostat("ours", 20.0, 1, 0, 0, 1);
        var z1 = new Zone(ts1, new ZoneSettings(ts1.getSetpoint()));

        var ts2 = new Thermostat("alien", 25.0, 1, 0, 0, 1);
        var z2 = new Zone(ts2, new ZoneSettings(ts2.getSetpoint()));

        // Note, no z2
        var zc = new ZoneController(Set.of(z1));

        var sequence = Flux
                .just(new Signal<Double, String>(atDayStartUTC(), 30.0));

        var flux1 = z1.compute(sequence);
        var flux2 = z2.compute(sequence);

        // Note concat(), order is important for StepVerifier
        var fluxZ = zc.compute(Flux.concat(flux1, flux2));

        z1.close();
        z2.close();

        // Note, flux2 never made it through
        StepVerifier
                .create(fluxZ)
                .assertNext(s -> assertThat(s.getValue().demand).isEqualTo(11.0))
                .verifyComplete();
    }

    /**
     * Make sure all zones are raised when the HVAC unit turns on.
     *
     * See <a href="https://github.com/home-climate-control/dz/issues/300">#300</a> for details.
     */
    @Test
    void raise() throws InterruptedException {

        var t1 = createRaiseTuple(20.0, "ts20");
        var t2 = createRaiseTuple(25.0, "ts25");
        var t3 = createRaiseTuple(30.0, "ts30");

        var tuples = Set.of(t1, t2, t3);
        var zones = Set.of(
                t1.zone,
                t2.zone,
                t3.zone
        );

        var zc = new ZoneController(zones);

        var z1 = t1.zone.compute(t1.flux).doOnNext(signal -> t1.output.add(signal.getValue()));
        var z2 = t2.zone.compute(t2.flux).doOnNext(signal -> t2.output.add(signal.getValue()));
        var z3 = t3.zone.compute(t3.flux).doOnNext(signal -> t3.output.add(signal.getValue()));

        var allZones = Flux.merge(z1, z2, z3);

        var output = zc
                .compute(allZones)
                .subscribeOn(Schedulers.boundedElastic());

        // Prime the zones

        // Almost unhappy
        t1.sink.tryEmitNext(createSignal(t1.setpoint + 0.9, t1.zone.getAddress()));
        t2.sink.tryEmitNext(createSignal(t2.setpoint + 0.9, t2.zone.getAddress()));

        // Quite happy
        t3.sink.tryEmitNext(createSignal(t3.setpoint - 2, t3.zone.getAddress()));

        var zcOutput = Collections.synchronizedList(new ArrayList<Signal<UnitControlSignal, String>>());

        // As many as there are zones
        var gate = new CountDownLatch(3);

        output
                .doOnNext(r -> logger.info("output: {}", r))
                .doOnNext(zcOutput::add)
                .subscribe(ignored -> gate.countDown());

        // Give the thread a chance to start
        gate.await();

        // Verify
        assertThat(t1.output.get(0).callingStatus().calling()).isFalse();
        assertThat(t2.output.get(0).callingStatus().calling()).isFalse();
        assertThat(t3.output.get(0).callingStatus().calling()).isFalse();

        assertThat(zcOutput).hasSize(3);
        assertThat(zcOutput.get(2).getValue().demand).isZero();

        // Now make one unhappy
        t1.sink.tryEmitNext(createSignal(t1.setpoint + 5, t1.zone.getAddress()));

        // Slow boxes struggle with ConcurrentModificationException; let's give them a bit of time to think until the right solution is implemented
        Thread.sleep(100);

        assertThat(zcOutput).hasSize(5);
        assertThat(zcOutput.get(3).getValue().demand).isEqualTo(6.0);

        // This one was responsible for changing the status
        assertThat(t1.output).hasSize(2);
        assertThat(t1.output.get(1).callingStatus().calling()).isTrue();

        // This one was raised
        assertThat(t2.output).hasSize(2);
        // But this one wasn't, no signals were emitted
        assertThat(t3.output).hasSize(1);

        assertThat(t2.output.get(1).callingStatus().calling()).isTrue();

    }

    /**
     * Reproducer for one of possible scenarios causing <a href="https://github.com/home-climate-control/dz/issues/333">#333</a>.
     */
    @Test
    void sensorDeparture1() throws Exception {

        final var setpointGood = 28.0;
        final var setpointBad = 29.0;

        var tsGood = new Thermostat(Clock.systemUTC(), "good-zone", new Range<>(10d, 40d), setpointGood, 1, 0, 0, 1, Duration.ZERO, 0);
        var zoneGood = new Zone(tsGood, new ZoneSettings(tsGood.getSetpoint()));

        var tsBad = new Thermostat(Clock.systemUTC(), "bad-zone", new Range<>(10d, 40d), setpointBad, 1, 0, 0, 1, Duration.ZERO, 0);
        var zoneBad = new Zone(tsBad, new ZoneSettings(tsBad.getSetpoint()));

        var zc = new ZoneController(Set.of(zoneGood, zoneBad));

        var start = atDayStartUTC();

        Sinks.Many<Signal<Double, String>> sinkGood = reactor.core.publisher.Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<Signal<Double, String>> sinkBad = reactor.core.publisher.Sinks.many().multicast().onBackpressureBuffer();

        var zoneFluxGood = zoneGood
                .compute(sinkGood.asFlux())
                .doOnNext(s -> logger.warn("signal/good:     {}", s));
        var zoneFluxBad = zoneBad
                .compute(sinkBad.asFlux())
                .doOnNext(s -> logger.warn("signal/bad:      {}", s));

        var aggregatedFlux = Flux
                .merge(zoneFluxGood, zoneFluxBad)
                .doOnNext(s -> logger.warn("signal/merged:   {}", s));

        var accumulator = new ArrayList<Signal<UnitControlSignal, String>>();
        var out = zc
                .compute(aggregatedFlux)
                .doOnNext(s -> logger.warn("signal/computed: {}", s))
                .subscribe(accumulator::add);

        // @0 both zones are happy, expect 0
        sinkGood.tryEmitNext(new Signal<Double, String>(start, setpointGood - 1));
        sinkBad.tryEmitNext(new Signal<Double, String>(start, setpointBad - 1));

        // @+2min, the good zone is unhappy, expect 2 demands of 3
        sinkGood.tryEmitNext(new Signal<Double, String>(start.plus(2, ChronoUnit.MINUTES), setpointGood + 2));

        // @+3min, the good zone is happy, expect 0
        sinkGood.tryEmitNext(new Signal<Double, String>(start.plus(3, ChronoUnit.MINUTES), setpointGood - 2));

        // @+4min, the sensor is gone in the bad zone, expect 0
        sinkBad.tryEmitNext(new Signal<Double, String>(start.plus(4, ChronoUnit.MINUTES), null, null, Signal.Status.FAILURE_TOTAL, new IllegalStateException("test")));

        // @+10min the good zone is unhappy, expect 2 demands of 3 but get 3 then 4
        sinkGood.tryEmitNext(new Signal<Double, String>(start.plus(10, ChronoUnit.MINUTES), setpointGood + 2));

        // @+20min the good zone is happy, expect 0 but get 1
        sinkGood.tryEmitNext(new Signal<Double, String>(start.plus(20, ChronoUnit.MINUTES), setpointGood - 2));

        // This causes the signal to replay and issue two signals
        zoneBad.setSettingsSync(new ZoneSettings(setpointBad - 3));

        // @+6hrs the sensor is back, the bad zone is very, very happy, but the occupants are not
        sinkBad.tryEmitNext(new Signal<Double, String>(start.plus(6, ChronoUnit.HOURS), setpointBad - 9));

        sinkGood.tryEmitComplete();
        sinkBad.tryEmitComplete();

        assertThat(accumulator).hasSize(11);

        // @0 both zones happy
        assertThat(accumulator.get(0).getValue().demand).isEqualTo(0.0);
        assertThat(accumulator.get(1).getValue().demand).isEqualTo(0.0);

        // @+2 good zone unhappy, expect 2 demands of 3
        assertThat(accumulator.get(2).getValue().demand).isEqualTo(3.0);
        assertThat(accumulator.get(3).getValue().demand).isEqualTo(3.0);

        // @+3 good zone happy, expect 0
        assertThat(accumulator.get(4).getValue().demand).isEqualTo(0.0);

        // @+4 bad zone sensor gone, expect 0
        assertThat(accumulator.get(5).getValue().demand).isEqualTo(0.0);

        // @+10 good zone unhappy, expect 3
        assertThat(accumulator.get(6).getValue().demand).isEqualTo(3.0);

        // @+20 good zone is happy, expect 0
        assertThat(accumulator.get(7).getValue().demand).isEqualTo(0.0);

        // setSettingsSync(), the zone should cause calling but is in error, demand goes to (0, 0)
        assertThat(accumulator.get(8).getValue().demand).isEqualTo(0.0);
        assertThat(accumulator.get(9).getValue().demand).isEqualTo(0.0);

        // @+6hrs the bad zone sensor returns, everyone's happy, expect 0
        assertThat(accumulator.get(10).getValue().demand).isEqualTo(0.0);

        zoneGood.close();
        zoneBad.close();

        out.dispose();
    }

    /**
     * Reproducer for one of possible scenarios causing <a href="https://github.com/home-climate-control/dz/issues/333">#333</a>.
     */
    @Test
    void sensorDeparture2() throws Exception {

        final var setpointGood = 28.0;
        final var setpointBad = 29.0;

        var tsGood = new Thermostat(Clock.systemUTC(), "good-zone", new Range<>(10d, 40d), setpointGood, 1, 0, 0, 1, Duration.ZERO, 0);
        var zoneGood = new Zone(tsGood, new ZoneSettings(tsGood.getSetpoint()));

        var tsBad = new Thermostat(Clock.systemUTC(), "bad-zone", new Range<>(10d, 40d), setpointBad, 1, 0, 0, 1, Duration.ZERO, 0);
        var zoneBad = new Zone(tsBad, new ZoneSettings(tsBad.getSetpoint()));

        var zc = new ZoneController(Set.of(zoneGood, zoneBad));

        var start = atDayStartUTC();

        Sinks.Many<Signal<Double, String>> sinkGood = reactor.core.publisher.Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<Signal<Double, String>> sinkBad = reactor.core.publisher.Sinks.many().multicast().onBackpressureBuffer();

        var zoneFluxGood = zoneGood
                .compute(sinkGood.asFlux())
                .doOnNext(s -> logger.warn("signal/good:     {}", s));
        var zoneFluxBad = zoneBad
                .compute(sinkBad.asFlux())
                .doOnNext(s -> logger.warn("signal/bad:      {}", s));

        var aggregatedFlux = Flux
                .merge(zoneFluxGood, zoneFluxBad)
                .doOnNext(s -> logger.warn("signal/merged:   {}", s));

        var accumulator = new ArrayList<Signal<UnitControlSignal, String>>();
        var out = zc
                .compute(aggregatedFlux)
                .doOnNext(s -> logger.warn("signal/computed: {}", s))
                .subscribe(accumulator::add);

        // @0 both zones are happy, expect 0
        sinkGood.tryEmitNext(new Signal<Double, String>(start, setpointGood - 1));
        sinkBad.tryEmitNext(new Signal<Double, String>(start, setpointBad - 1));

        // @+1min, the sensor is gone in the bad zone, expect 0
        sinkBad.tryEmitNext(new Signal<Double, String>(start.plus(1, ChronoUnit.MINUTES), null, null, Signal.Status.FAILURE_TOTAL, new IllegalStateException("test")));

        // The new setpoint is deeply below the last known good sensor reading, but the zone is in error, expect (0, 0)
        zoneBad.setSettingsSync(new ZoneSettings(setpointBad - 5));

        // @+2min, the good zone is unhappy, expect 3
        sinkGood.tryEmitNext(new Signal<Double, String>(start.plus(2, ChronoUnit.MINUTES), setpointGood + 2));

        // @+3min, the good zone is happy, expect 0
        sinkGood.tryEmitNext(new Signal<Double, String>(start.plus(3, ChronoUnit.MINUTES), setpointGood - 2));

        // @+10min the good zone is unhappy, the demand is wrong
        sinkGood.tryEmitNext(new Signal<Double, String>(start.plus(10, ChronoUnit.MINUTES), setpointGood + 2));

        // @+20min the good zone is happy, expect 0
        sinkGood.tryEmitNext(new Signal<Double, String>(start.plus(20, ChronoUnit.MINUTES), setpointGood - 2));

        // @+6hrs the sensor is back, the bad zone is very, very happy, but the occupants are not
        sinkBad.tryEmitNext(new Signal<Double, String>(start.plus(6, ChronoUnit.HOURS), setpointBad - 9));

        sinkGood.tryEmitComplete();
        sinkBad.tryEmitComplete();

        assertThat(accumulator).hasSize(10);

        // @0 both zones happy
        assertThat(accumulator.get(0).getValue().demand).isEqualTo(0.0);
        assertThat(accumulator.get(1).getValue().demand).isEqualTo(0.0);

        // @+1 bad zone sensor gone, expect 0
        assertThat(accumulator.get(2).getValue().demand).isEqualTo(0.0);

        // Change of setpoint; the zone is error, zero demand
        assertThat(accumulator.get(3).getValue().demand).isEqualTo(0.0);
        assertThat(accumulator.get(4).getValue().demand).isEqualTo(0.0);

        // @+2min, the good zone is unhappy, expect 3
        assertThat(accumulator.get(5).getValue().demand).isEqualTo(3.0);

        // @+3min, the good zone is happy, expect 0
        assertThat(accumulator.get(6).getValue().demand).isEqualTo(0.0);

        // @+10min the good zone is unhappy
        assertThat(accumulator.get(7).getValue().demand).isEqualTo(3.0);

        // @+20min the good zone is happy, expect 0
        assertThat(accumulator.get(8).getValue().demand).isEqualTo(0.0);

        // @+6hrs the sensor is back, the bad zone is very, very happy, but the occupants are not
        assertThat(accumulator.get(9).getValue().demand).isEqualTo(0.0);

        zoneGood.close();
        zoneBad.close();

        out.dispose();
    }

    private Signal<Double, String> createSignal(double temperature, String address) {
        return new Signal<>(Instant.now(), temperature, address);
    }

    private RaiseTuple createRaiseTuple(double setpoint, String zoneName) {

        Sinks.Many<Signal<Double, String>> sink = Sinks.many().multicast().onBackpressureBuffer();

        return new RaiseTuple(
                setpoint,
                new Zone(
                        new Thermostat(zoneName, setpoint, 1, 0, 0, 1),
                        new ZoneSettings(true, setpoint, true, false, 0, null)
                ),
                sink,
                sink.asFlux(),
                Collections.synchronizedList(new ArrayList<>()));
    }

    private record RaiseTuple(
            double setpoint,
            Zone zone,
            Sinks.Many<Signal<Double, String>> sink,
            Flux<Signal<Double, String>> flux,
            List<ZoneStatus> output
    ) {

    }
}
