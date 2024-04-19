package net.sf.dz3r.runtime.config.model;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.device.actuator.economizer.EconomizerContext;
import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import net.sf.dz3r.model.Range;
import net.sf.dz3r.model.Thermostat;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import org.apache.commons.lang3.tuple.ImmutablePair;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ZoneConfigurationParser extends ConfigurationContextAware {

    public ZoneConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<ZoneConfig> source) {

        var nonNullSource = Optional.ofNullable(source).orElse(Set.of());

        if (nonNullSource.isEmpty()) {

            // There's a slight chance that they wanted to have just the sensors, let them be
            logger.warn("No zones configured, are you sure?");
        }

        Flux
                .fromIterable(nonNullSource)
                .map(this::createZone)
                .subscribe(kv -> context.zones.register(kv.getKey(), kv.getValue()));
    }

    private Map.Entry<String, Zone> createZone(ZoneConfig cf) {

        var ts = createThermostat(cf.name(), cf.settings().setpoint(), cf.settings().setpointRange(), cf.controller());
        var eco = createEconomizer(cf.name(), cf.economizer());
        var ecoSettings = Optional.ofNullable(eco).map(v -> v.config.settings).orElse(null);
        var zone = new Zone(ts, map(cf.settings(), ecoSettings), eco);

        return new ImmutablePair<>(cf.id(), zone);
    }

    private EconomizerContext createEconomizer(String zoneName, net.sf.dz3r.runtime.config.model.EconomizerConfig cf) {

        if (cf == null) {
            return null;
        }

        var ambientSensor = HCCObjects.requireNonNull(getSensorBlocking(cf.ambientSensor()), "can't resolve ambient-sensor=" + cf.ambientSensor());
        var hvacDevice = HCCObjects.requireNonNull(getHvacDevice(cf.hvacDevice()), "can't resolve hvac-device=" + cf.hvacDevice());
        var timeout = Optional.ofNullable(cf.timeout()).orElseGet(() -> {
            var t = Duration.ofSeconds(90);
            logger.info("{}: using default stale timeout of {} for the economizer", zoneName, t);
            return t;
        });

        if (cf.settings() == null) {

            // This is possibly a result of a breaking change in the configuration at rev. a05d2593c12f6821a7b0c6e10b41808d04544a01.
            // But rev. 6befbc1afde887de526439e93bc7762eb25f4a1b unbrokeded the change, and now missing settings are OK, so this is just a warning.
            // It'll go away after the transition period is over.

            logger.warn("null settings, have you updated the economizer configuration? Here's what it looks like: {}", cf);
            logger.warn("see https://github.com/home-climate-control/dz/blob/master/docs/configuration/zones.md#economizer for more information");
        }

        // VT: NOTE: maxPower is only configurable via UI and scheduler - there's no sense in setting it here, semantics are unclear

        return new EconomizerContext(
                new EconomizerConfig(
                        cf.mode(),
                        cf.controller().p(),
                        cf.controller().i(),
                        cf.controller().limit(),
                        Optional
                                .ofNullable(cf.settings())
                                .map(settings -> new EconomizerSettings(
                                        settings.changeoverDelta(),
                                        settings.targetTemperature(),
                                        settings.keepHvacOn(),
                                        1.0))
                                .orElse(null)
                ),
                ambientSensor,
                hvacDevice,
                timeout);
    }

    private Thermostat createThermostat(String name, Double setpoint, RangeConfig rangeConfig, PidControllerConfig cf) {

        var range = map(Optional.ofNullable(rangeConfig).orElse(new RangeConfig(10.0, 40.0)));
        return new Thermostat(Clock.systemUTC(), name, range, setpoint, cf.p(), cf.i(), cf.d(), cf.limit());
    }

    private ZoneSettings map(ZoneSettingsConfig source, EconomizerSettings economizerSettings) {

        return new ZoneSettings(
                Optional.ofNullable(source.enabled()).orElse(true),
                source.setpoint(),
                Optional.ofNullable(source.voting()).orElse(true),
                Optional.ofNullable(source.hold()).orElse(false),
                source.dumpPriority(),
                economizerSettings);
    }

    private Range<Double> map(RangeConfig cf) {
        return new Range<>(cf.min(), cf.max());
    }
}
