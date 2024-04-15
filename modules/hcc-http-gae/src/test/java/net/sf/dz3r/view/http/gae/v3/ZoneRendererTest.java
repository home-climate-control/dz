package net.sf.dz3r.view.http.gae.v3;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.PeriodSettings;
import net.sf.dz3r.model.SchedulePeriod;
import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.CallingStatus;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneRendererTest {

    private final Logger logger = LogManager.getLogger();
    private final ZoneRenderer zr = new ZoneRenderer();

    private static final String UNIT_COOLING = "unit-cooling";
    private static final String ZONE_A = "zone-A";

    @BeforeEach
    void setModes() {
        var now = Instant.now();

        zr.consumeMode(new Signal<>(now, HvacMode.COOLING, UNIT_COOLING));
        zr.consumeSensorSignal(new Signal<>(now, 25d, ZONE_A));
    }

    @Test
    void deviation() {

        // Abbreviated constructor - sets enabled = true, voting = true
        var settingsActual = new ZoneSettings(25);

        // Full constructor - explicit values
        var settingsPeriod = new ZoneSettings(
                null,
                25d,
                null,
                null,
                null,
                null);

        logger.debug("settings/actual: {}", settingsActual);
        logger.debug("settings/period: {}", settingsPeriod);

        var status = new ZoneStatus(
                settingsActual,
                new CallingStatus(0d, 0, false),
                null,
                new PeriodSettings(
                        new SchedulePeriod("id", "name", LocalTime.MIDNIGHT, LocalTime.NOON, (byte) 0x00),
                        settingsPeriod
                )
        );
        var signal = new Signal<ZoneStatus, String>(Instant.now(), status, ZONE_A);
        var result = zr.compute(UNIT_COOLING, Flux.just(signal)).blockFirst(); // NOSONAR False positive, this is a test

        logger.debug("result: {}", result);

        assertThat(result).isNotNull();

        // As of rev. 64222dec3ef937f9cd0212355077d68380553441, the above will result in deviation on enabled and voting, hence fail
        assertThat(result.deviationEnabled).isFalse();
        assertThat(result.deviationVoting).isFalse();
    }
}
