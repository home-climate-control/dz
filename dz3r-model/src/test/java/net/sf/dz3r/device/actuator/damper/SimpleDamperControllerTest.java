package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.model.Thermostat;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.CallingStatus;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@Disabled("Stalls CI/CD jobs, why?")
class SimpleDamperControllerTest {

    private final Logger logger = LogManager.getLogger();
    private final Random rg = new SecureRandom();

    @Test
    void testEmptyMapping() {

        assertThatIllegalStateException()
                .isThrownBy(() -> new SimpleDamperController(Map.of()))
                .withMessage("No zone to damper mapping provided, need at least two pairs");
    }

    /**
     * Make sure all damper controllers refuse to exist if just one zone/damper pair is provided.
     * Just skip it in that case, all right.
     */
    @Test
    void testSingleZone() {

        assertThatIllegalStateException()
                .isThrownBy(() -> {

                    var zoneSettings = new ZoneSettings(20);
                    var z1 = new Zone(new Thermostat("Z1", 20, 1, 0, 0, 1), zoneSettings);

                    var d1 = new NullDamper("d1");

                    new SimpleDamperController(Map.of(z1, d1));
                })
                .withMessage("Damper controller with just one damper doesn't make sense. Just skip it altogether.");
    }

    /**
     * Make sure that thermostats with negative demand don't cause damper control signals
     * out of acceptable range.
     */
    @Test
    void testBoundaries1() {

        assertThatCode(() -> {

            var zoneSettings = new ZoneSettings(20);
            var z1 = new Zone(new Thermostat("Z1", 20, 1, 0, 0, 1), zoneSettings);
            var z2 = new Zone(new Thermostat("Z2", 20, 1, 0, 0, 1), zoneSettings);

            var d1 = new NullDamper("d1");
            var d2 = new NullDamper("d2");

            var park1 = rg.nextDouble();
            var park2 = rg.nextDouble();

            d1.setParkPosition(park1);
            d2.setParkPosition(park2);

            try (SimpleDamperController damperController = new SimpleDamperController(Map.of(
                    z1, d1,
                    z2, d2
            ))) {

                var unitFlux = Flux
                        .just(new Signal<UnitControlSignal, Void>(Instant.now(), new UnitControlSignal(0d, 0d)));

                var status1 = new ZoneStatus(zoneSettings, new CallingStatus(50, true), null);
                var status2 = new ZoneStatus(zoneSettings, new CallingStatus(-50, false), null);
                var signalFlux = Flux.just(
                        new Signal<>(Instant.now(), status1, "Z1", Signal.Status.OK, null),
                        new Signal<>(Instant.now(), status2, "Z2", Signal.Status.OK, null)
                ).log();

                int eventCount = 2;
                var gate = new CountDownLatch(eventCount);

                damperController
                        .compute(unitFlux, signalFlux)
                        .take(eventCount)
                        .doOnNext(ignored -> gate.countDown())
                        .blockLast();

                logger.info("waiting for the gate...");
                gate.await();
                logger.info("past the gate");

                logger.info("d1: {}", d1);
                logger.info("d2: {}", d2);

                assertThat(d1.get()).isEqualTo(park1);
                assertThat(d2.get()).isEqualTo(park2);
            }

        }).doesNotThrowAnyException();
    }

    /**
     * Make sure that thermostats with negative demand don't cause damper control signals
     * out of acceptable range.
     */
    @Test
    void testBoundaries2() {

        assertThatCode(() -> {

            var zoneSettings = new ZoneSettings(20);
            var z1 = new Zone(new Thermostat("Z1", 20, 1, 0, 0, 1), zoneSettings);
            var z2 = new Zone(new Thermostat("Z2", 20, 1, 0, 0, 1), zoneSettings);

            var d1 = new NullDamper("d1");
            var d2 = new NullDamper("d2");

            try (SimpleDamperController damperController = new SimpleDamperController(Map.of(
                    z1, d1,
                    z2, d2
            ))) {

                var unitFlux = Flux
                        .just(new Signal<UnitControlSignal, Void>(Instant.now(), new UnitControlSignal(1d, 1d)));

                var status1 = new ZoneStatus(zoneSettings, new CallingStatus(50, true), null);
                var status2 = new ZoneStatus(zoneSettings, new CallingStatus(-50, false), null);
                var signalFlux = Flux.just(
                        new Signal<>(Instant.now(), status1, "Z1", Signal.Status.OK, null),
                        new Signal<>(Instant.now(), status2, "Z2", Signal.Status.OK, null)
                ).log();


                int eventCount = 3;
                var gate = new CountDownLatch(eventCount);

                damperController
                        .compute(unitFlux, signalFlux)
                        .take(eventCount)
                        .doOnNext(ignored -> gate.countDown())
                        .blockLast();

                logger.info("waiting for the gate...");
                gate.await();
                logger.info("past the gate");

                logger.info("d1: {}", d1);
                logger.info("d2: {}", d2);

                assertThat(d1.get()).isEqualTo(1d);
                assertThat(d2.get()).isEqualTo(0d);
            }

        }).doesNotThrowAnyException();
    }

    /**
     * Make sure that zero demand from all thermostats doesn't cause NaN sent to dampers.
     */
    @Test
    void testNaN() {

        assertThatCode(() -> {

            var zoneSettings = new ZoneSettings(20);
            var z1 = new Zone(new Thermostat("Z1", 20, 1, 0, 0, 1), zoneSettings);
            var z2 = new Zone(new Thermostat("Z2", 20, 1, 0, 0, 1), zoneSettings);

            var d1 = new NullDamper("d1");
            var d2 = new NullDamper("d2");

            try (SimpleDamperController damperController = new SimpleDamperController(Map.of(
                    z1, d1,
                    z2, d2
            ))) {

                var unitFlux = Flux
                        .just(new Signal<UnitControlSignal, Void>(Instant.now(), new UnitControlSignal(1d, 1d)));

                var status1 = new ZoneStatus(zoneSettings, new CallingStatus(-50, false), null);
                var status2 = new ZoneStatus(zoneSettings, new CallingStatus(-50, false), null);
                var signalFlux = Flux.just(
                        new Signal<>(Instant.now(), status1, "Z1", Signal.Status.OK, null),
                new Signal<>(Instant.now(), status2, "Z2", Signal.Status.OK, null)
                ).log();

                int eventCount = 2;
                var gate = new CountDownLatch(eventCount);

                damperController
                        .compute(unitFlux, signalFlux)
                        .take(eventCount)
                        .doOnNext(ignored -> gate.countDown())
                        .blockLast();

                // Let them settle
                Thread.sleep(100);

                logger.info("waiting for the gate...");
                gate.await();
                logger.info("past the gate");

                logger.info("d1: {}", d1);
                logger.info("d2: {}", d2);

                // VT: NOTE: This test case matches the "existing code", but it is bizarre - a running unit with
                // identical negative demand shouldn't ever get anything other than 1.0 on any damper.
                // Bug report coming up.

                assertThat(d1.get()).isEqualTo(0d);
                assertThat(d2.get()).isEqualTo(0d);
            }

        }).doesNotThrowAnyException();
    }
}
