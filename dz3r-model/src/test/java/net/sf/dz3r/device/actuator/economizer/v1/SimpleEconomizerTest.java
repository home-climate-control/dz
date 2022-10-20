package net.sf.dz3r.device.actuator.economizer.v1;

import net.sf.dz3r.device.actuator.NullSwitch;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.Thermostat;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Instant;

class SimpleEconomizerTest {

    private final Logger logger = LogManager.getLogger();
    private FluxSink<Signal<Double, Void>> ambientSink;

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    /**
     * Make sure that the economizer turns on when the ambient temperature falls below its trigger point,
     * and then turns off when it raises again.
     */
    @Test
    void dropToOnAndBack() {

        // Target temperature is below the lowest in the ambient flux,
        // the economizer will just turn on and off
        var config = new EconomizerConfig(
                HvacMode.COOLING,
                2,
                10);

        var indoor = 25.0;

        // Irrelevant, economizer overrides it
        var setpoint = 32.0;

        var targetZone = new Zone(
                new Thermostat("ts", setpoint, 1, 0, 0, 1),
                new ZoneSettings(setpoint));

        // VT: FIXME: Replace with a mock to verify()
        var targetDevice = new NullSwitch("s");

        // Can't feed it right away, it will all be consumed within the constructor
        var ambientFlux = getAmbientFlux();
        var deferredAmbientFlux = Flux.create(this::connectAmbient);

        var economizer = new SimpleEconomizer<String>(
                config,
                targetZone,
                deferredAmbientFlux,
                targetDevice);

        economizer
                .compute(Flux.just(new Signal<>(Instant.now(), indoor)))
                .blockLast();

        ambientFlux
                .doOnNext(ambientSink::next)
                .blockLast();
    }

    private void connectAmbient(FluxSink<Signal<Double, Void>> ambientSink) {
        this.ambientSink = ambientSink;
    }

    /**
     * Make sure that the economizer turns on when the ambient temperature falls below its trigger point,
     * then turns off when the indoor temperature falls below the target temperature,
     * and then turns on and then off when it raises again.
     */
    @Test
    @Disabled
    void dropToBelowTargetAndBack() {

        // Target temperature is within the range of the ambient flux,
        // the economizer will turn on, then off, then on and off again
        var config = new EconomizerConfig(
                HvacMode.COOLING,
                2,
                18);

        var setpoint = 25;
        var ambient = getAmbientFlux();
    }

    /**
     * Get a flux of values 1 apart from 30 to 15 and back to 30.
     */
    private Flux<Signal<Double, Void>> getAmbientFlux() {
        return Flux.concat(
                        Flux.range(0, 15).map(t -> 30 - t),
                        Flux.range(15, 16))
                .map(Integer::doubleValue)
                .doOnNext(t -> logger.info("emit ambient={}", t))
                .map(t  -> new Signal<Double, Void>(Instant.now(), t));
    }
}
