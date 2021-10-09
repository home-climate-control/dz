package net.sf.dz3r.device.actuator.damper;

import net.sf.dz3r.model.Thermostat;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ThermostatStatus;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

class BalancingDamperControllerTest {

    private final Logger logger = LogManager.getLogger();

    /**
     * Make sure that thermostats with negative demand don't cause damper control signals
     * out of acceptable range.
     */
    @Test
    void testBoundaries() {

        assertThatCode(() -> {

            var zoneSettings = new ZoneSettings(20);
            var z1 = new Zone(new Thermostat("Z1", 20, 1, 0, 0, 1), zoneSettings);
            var z2 = new Zone(new Thermostat("Z2", 20, 1, 0, 0, 1), zoneSettings);

            var d1 = new NullDamper("d1");
            var d2 = new NullDamper("d2");

            try (BalancingDamperController damperController = new BalancingDamperController(Map.of(
                    z1, d1,
                    z2, d2
            ))) {

                var unitFlux = Flux
                        .just(new Signal<UnitControlSignal, Void>(Instant.now(), new UnitControlSignal(0d, 0d)));

                var status1 = new ZoneStatus(zoneSettings, new ThermostatStatus(50, true));
                var status2 = new ZoneStatus(zoneSettings, new ThermostatStatus(-50, true));
                var signalFlux = Flux.just(
                        new Signal<>(Instant.now(), status1, "Z1", Signal.Status.OK, null),
                        new Signal<>(Instant.now(), status2, "Z2", Signal.Status.OK, null)
                ).log();

                damperController
                        .compute(unitFlux, signalFlux)
                        .take(2)
                        .subscribe(s -> logger.error("output: {}", s));
            }

        }).doesNotThrowAnyException();
    }
}
