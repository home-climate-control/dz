package net.sf.dz3r.runtime.config;

import com.homeclimatecontrol.hcc.hvac.PsychrometricsTool;
import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.Enthalpy;
import net.sf.dz3r.device.actuator.CqrsSwitch;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.VariableOutputDevice;
import net.sf.dz3r.model.UnitController;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.runtime.config.model.SensorMappingConfig;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.homeclimatecontrol.hcc.hvac.PsychrometricsTool.STANDARD_ATMOSPHERIC_PRESSURE;

public abstract class ConfigurationContextAware {

    protected final Logger logger = LogManager.getLogger();
    protected final ConfigurationContext context;

    protected ConfigurationContextAware(ConfigurationContext context) {
        this.context = context;
    }

    protected final Flux<Signal<Double, Void>> getSensorBlocking(String address) {
        return Optional
                .ofNullable(getSensor(address).block())
                .orElse(Flux.error(new IllegalArgumentException("Couldn't resolve sensor flux for id or address '" + address + "'")));
    }
    protected final Mono<Flux<Signal<Double, Void>>> getSensor(String address) {
        return context
                .sensors
                .getMonoById("sensors", address)
                .doOnNext(s -> logger.debug("getSensor({}) = {}", address, s));
    }

    protected final CqrsSwitch<?> getSwitch(String address) {
        return context
                .switches
                .getMonoById("switches", address)
                .block();
    }

    protected final VariableOutputDevice getFans(String address) {
        return context
                .fans
                .getMonoById("fans", address)
                .block();
    }

    protected final Zone getZone(String address) {
        return context
                .zones
                .getMonoById("zones", address)
                .block();
    }

    protected final Map<Flux<Signal<Double, Void>>, Zone> getSensorFeed2ZoneMapping(Map<String, String> source) {

        if (!source.isEmpty()) {
            logger.warn("'sensor-feed-mapping' is deprecated, use 'sensor-mapping' instead");
        }

        return Flux
                .fromIterable(source.entrySet())
                .map(kv -> {

                    var flux = getSensorBlocking(kv.getKey());
                    var zone = getZone(kv.getValue());

                    return new ImmutablePair<>(flux, zone);
                })
                .collectMap(Pair::getKey, Pair::getValue)
                .block();
    }

    protected final Map<Flux<Signal<Enthalpy, Void>>, Zone> getSensorMapping(Map<String, SensorMappingConfig> source) {

        return Flux
                .fromIterable(source.entrySet())
                .map(kv -> {

                    var zone = getZone(kv.getKey());

                    // This sensor is mandatory

                    var sensorT = kv.getValue().temperature();
                    var sensorH = kv.getValue().humidity();
                    var sensorP = kv.getValue().atmosphericPressure();

                    ThreadContext.push("sensorMapping: " + zone.getAddress());
                    logger.debug("temperature: {}", sensorT);
                    logger.debug("humidity: {}", sensorH);
                    logger.debug("pressure: {}", sensorP);
                    ThreadContext.pop();

                    if (sensorH == null) {
                        // Can't calculate enthalpy without humidity, so we'll have to fudge
                        logger.warn("no humidity sensor configured for zone {}, assuming RH=50%", zone);
                    }

                    if (sensorP == null) {
                        // Enthalpy won't change that much with pressure, so we'll just take the standard
                        logger.warn("no pressure sensor configured for zone {}, using standard {}hPa", zone, STANDARD_ATMOSPHERIC_PRESSURE);
                    }

                    var fluxT = getSensorBlocking(kv.getValue().temperature());
                    var fluxH = sensorH == null ? Flux.<Signal<Double, Void>>empty() : getSensorBlocking(sensorH);
                    var fluxP = sensorP == null ? Flux.<Signal<Double, Void>>empty() : getSensorBlocking(sensorP);

                    return new ImmutablePair<>(getEnthalpyFlux(fluxT, fluxH, fluxP), zone);
                })
                .collectMap(Pair::getKey, Pair::getValue)
                .block();
    }

    private Flux<Signal<Enthalpy, Void>> getEnthalpyFlux(
            Flux<Signal<Double, Void>> fluxT,
            Flux<Signal<Double, Void>> fluxH,
            Flux<Signal<Double, Void>> fluxP) {

        var safeH = fluxH.map(Optional::of).defaultIfEmpty(Optional.empty());
        var safeP = fluxP.map(Optional::of).defaultIfEmpty(Optional.empty());

        return Flux
                .zip(fluxT, safeH, safeP)
                .map(tuple -> calculateEnthalpy(
                        tuple.getT1(),
                        tuple.getT2().orElse(null),
                        tuple.getT3().orElse(null)
                ));
    }

    private Signal<Enthalpy, Void> calculateEnthalpy(
            Signal<Double, Void> t,
            Signal<Double, Void> h,
            Signal<Double, Void> p) {

        // Reason for this complexity: enthalpy calculation must be independent of humidity and pressure reporting down the pipeline.
        // Hence, default values are substituted when the enthalpy is calculated, but nulls are passed down for reporting.

        return new Signal<>(
                getLatest(
                        t.timestamp(),
                        h == null ? null : h.timestamp(),
                        p == null ? null : p.timestamp()),
                new Enthalpy(
                PsychrometricsTool.calculateEnthalpy(
                        t.getValue(),
                        h == null ? 0.5 : h.getValue(),
                        p == null ? STANDARD_ATMOSPHERIC_PRESSURE : p.getValue()),
                t.getValue(),
                h == null ? null : h.getValue(),
                p == null ? null : p.getValue())
        );
    }

    private Instant getLatest(Instant t, Instant h, Instant p) {
        Objects.requireNonNull(t, "t cannot be null");

        return Stream.of(t, h, p)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(t);
    }

    protected final HvacDevice getHvacDevice(String address) {
        return context
                .hvacDevices
                .getMonoById("hvac", address)
                .block();
    }

    protected final UnitController getUnitController(String address) {
        return context
                .units
                .getMonoById("units", address)
                .block();
    }

    protected final boolean isConfigured(String source, Set<String> names, Map.Entry<String, ?> configured) {

        if (names == null || names.isEmpty()) {
            logger.warn("{} is missing, assuming all configured, returning: {}",
                    source,
                    Optional.ofNullable(configured).map(Map.Entry::getKey).orElse(null));

            return true;
        }

        return names.contains(configured.getKey());
    }
}
