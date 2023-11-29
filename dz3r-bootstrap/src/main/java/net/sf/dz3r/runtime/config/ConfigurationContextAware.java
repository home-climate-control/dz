package net.sf.dz3r.runtime.config;

import net.sf.dz3r.device.actuator.CqrsSwitch;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.VariableOutputDevice;
import net.sf.dz3r.model.UnitController;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
