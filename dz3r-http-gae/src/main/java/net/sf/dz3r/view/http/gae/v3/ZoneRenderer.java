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
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public class ZoneRenderer extends EntityRenderer<ZoneStatus, String> {

    private final Logger logger = LogManager.getLogger();

    /**
     * @see #consumeSensorSignal(Signal)
     */
    private final Map<String, Signal<Double, String>> zone2signal = new TreeMap<>();

    /**
     * @see #consumeMode(Signal)
     */
    private final Map<String, Signal<HvacMode, String>> unit2mode = new TreeMap<>();

    @Override
    public Flux<ZoneSnapshot> compute(String unitId, Flux<Signal<ZoneStatus, String>> in) {
        return in.flatMap(s -> convert(unitId, s));
    }

    private Flux<ZoneSnapshot> convert(String unitId, Signal<ZoneStatus, String> source) {

        var zoneName = source.payload;

        if (!unit2mode.containsKey(unitId)) {
            logger.debug("{}: don't know the mode yet, skipping this status: {}", unitId, source);
            return Flux.empty();
        }

        if (!zone2signal.containsKey(zoneName)) {
            logger.debug("{}: don't know the signal yet, skipping this status: {}", zoneName, source);
            return Flux.empty();
        }

        var status = source.getValue();
        var periodName = Optional.ofNullable(status.periodSettings).map(ps -> ps.period().name).orElse(null);
        var deviationSetpoint = Optional.ofNullable(status.periodSettings).map(ps -> status.settings.setpoint - ps.settings().setpoint).orElse(0d);
        var deviationEnabled = Optional.ofNullable(status.periodSettings).map(ps -> !Objects.equals(status.settings.enabled, ps.settings().enabled)).orElse(false);
        var deviationVoting = Optional.ofNullable(status.periodSettings).map(ps -> !Objects.equals(status.settings.voting, ps.settings().voting)).orElse(false);


        if (source.isError()) {
            logger.error("Not implemented: reporting error signal: {}", source);
            return Flux.empty();
        } else {

            try {
                return Flux.just(new ZoneSnapshot(
                        source.timestamp.toEpochMilli(),
                        zoneName,
                        modeMap.get(unit2mode.get(unitId).getValue()),
                        renderState(status.settings.enabled, status.callingStatus.calling),
                        status.callingStatus.demand,
                        zone2signal.get(zoneName).getValue(),
                        status.settings.setpoint,
                        status.settings.enabled,
                        status.settings.hold,
                        status.settings.voting,
                        periodName,
                        deviationSetpoint,
                        deviationEnabled,
                        deviationVoting,
                        null
                ));
            } catch (Exception ex) {
                logger.warn("hvacMode={}", unit2mode);
                logger.warn("sensorSignal={}", zone2signal);
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

    public void consumeSensorSignal(Signal<Double, String> signal) {

        // Signals from different zones are coming, must keep them separate
        zone2signal.put(signal.payload, signal);
        logger.debug("signal: {}={}", signal.payload, signal);
    }

    public void consumeMode(Signal<HvacMode, String> signal) {

        // Signals from different units are coming, must keep them separate
        logger.debug("Mode: {}={}", signal.payload, signal);
        unit2mode.put(signal.payload, signal);
    }
}
