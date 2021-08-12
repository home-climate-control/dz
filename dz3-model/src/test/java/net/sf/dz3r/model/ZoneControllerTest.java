package net.sf.dz3r.model;

import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * See {@code net.sf.dz3.device.model.impl.SimpleZoneControllerTest}
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
class ZoneControllerTest {

    /**
     * Simplest possible configuration: one thermostat.
     */
    @Test
    @Disabled("Not implemented yet")
    void test1H() {

        var offset = new AtomicInteger();
        var sequence = Flux.just(
                20.0,
                20.5,
                21.0,
                20.5,
                20.0,
                19.5,
                19.0
        ).map(e -> new Signal<Double, String>(Instant.now().plus(offset.getAndIncrement(), ChronoUnit.SECONDS), e));

        var ts = new Thermostat("ts", 20, 1, 0, 0, 1);
        var z = new Zone(ts, new ZoneSettings(ts.getSetpoint()));
        var zc = new ZoneController();

        var stage1 = z.compute(sequence);
        var stage2 = zc.compute(stage1).log();

        stage2.subscribe().dispose();
    }

    /**
     * Test the <a href="https://code.google.com/archive/p/diy-zoning/issues/1">"Cold Start" bug</a>.
     *
     * The zone controller should stay off without exceptions when the first ever signal
     * doesn't indicate calling.
     */
    @Test
    void testColdStartNotCalling() {
    }

    /**
     * Test the <a href="https://code.google.com/archive/p/diy-zoning/issues/1">"Cold Start" bug</a>.
     *
     * The zone controller should switch on when the first ever thermostat signal
     * indicates calling.
     */
    @Test
    void testColdStartCalling() {
    }
}
