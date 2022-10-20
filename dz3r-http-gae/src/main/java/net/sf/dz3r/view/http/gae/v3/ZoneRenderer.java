package net.sf.dz3r.view.http.gae.v3;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import net.sf.dz3r.view.http.gae.v3.wire.ZoneSnapshot;
import net.sf.dz3r.view.http.gae.v3.wire.ZoneState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Map;

public class ZoneRenderer extends EntityRenderer<ZoneStatus, String> {

    private final Logger logger = LogManager.getLogger();

    /**
     * @see #consumeSensorSignal(Signal)
     */
    private Signal<Double, String> sensorSignal;

    /**
     * @see #consumeMode(Signal)
     */
    private HvacMode hvacMode;

    @Override
    public Flux<ZoneSnapshot> compute(Flux<Signal<ZoneStatus, String>> in) {
        return in.flatMap(this::convert);
    }

    private Flux<ZoneSnapshot> convert(Signal<ZoneStatus, String> source) {

        if (hvacMode == null) {
            logger.warn("Don't know the mode yet, skipping this status: {}", source);
            return Flux.empty();
        }

        if (sensorSignal == null) {
            logger.warn("Don't know the signal yet, skipping this status: {}", source);
            return Flux.empty();
        }

        var zoneName = source.payload;
        var status = source.getValue();

        if (source.isError()) {
            logger.error("Not implemented: reporting error signal: {}", source);
            return Flux.empty();
        } else {

            try {
                return Flux.just(new ZoneSnapshot(
                        source.timestamp.toEpochMilli(),
                        zoneName,
                        modeMap.get(hvacMode),
                        renderState(status.settings.enabled, status.thermostatStatus.calling),
                        status.thermostatStatus.demand,
                        sensorSignal.getValue(),
                        status.settings.setpoint,
                        status.settings.enabled,
                        status.settings.hold,
                        status.settings.voting,
                        null,
                        0.0,
                        false,
                        false,
                        null
                ));
            } catch (Exception ex) {
                logger.warn("hvacMode={}", hvacMode);
                logger.warn("sensorSignal={}", sensorSignal);
                logger.warn("source={}", source);
                logger.error("Failed to create zone snapshot, dropped (see logs right above for context)", ex);
                return Flux.empty();
            }
        }
    }

    private ZoneState renderState(boolean on, boolean calling) {

        if (!on) {
            return ZoneState.OFF;
        }

        return calling ? ZoneState.CALLING : ZoneState.HAPPY;
    }

    private static final Map<HvacMode, net.sf.dz3r.view.http.gae.v3.wire.HvacMode> modeMap = Map.of(
            HvacMode.COOLING, net.sf.dz3r.view.http.gae.v3.wire.HvacMode.COOLING,
            HvacMode.HEATING, net.sf.dz3r.view.http.gae.v3.wire.HvacMode.HEATING
    );

    public void subscribeSensor(Flux<Signal<Double, String>> sensorFlux) {
        sensorFlux.subscribe(this::consumeSensorSignal);
    }

    private void consumeSensorSignal(Signal<Double, String> sensorSignal) {
        this.sensorSignal = sensorSignal;
        logger.debug("sensorSignal: {}", sensorSignal);
    }

    public void subscribeMode(Flux<Signal<HvacMode, Void>> in) {
        in.doOnNext(this::consumeMode).subscribe();
    }

    private void consumeMode(Signal<HvacMode, Void> signal) {
        logger.debug("Mode: {}", signal.getValue());
        this.hvacMode = signal.getValue();
    }
}
