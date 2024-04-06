package net.sf.dz3r.runtime.config.model;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.actuator.economizer.EconomizerConfig;
import net.sf.dz3r.device.actuator.economizer.EconomizerContext;
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
        var zone = new Zone(ts, map(cf.settings()), eco);

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

            // This is likely a result of a breaking change in the configuration, let's complain loudly but not explode
            // PS: It might be a good idea to remove this check a bit later when the users do update the configuration
            // PPS: Or just pretend it doesn't exist and configure the economizer inactive without them, see https://github.com/home-climate-control/dz/issues/267

            logger.error("null settings, have you updated the configuration? Here's what it looks like: {}", cf);
            logger.error("Ignoring economizer configuration for now");

            return null;
        }

        return new EconomizerContext(
                new EconomizerConfig(
                        cf.mode(),
                        cf.settings().changeoverDelta(),
                        cf.settings().targetTemperature(),
                        cf.settings().keepHvacOn(),
                        cf.controller().p(),
                        cf.controller().i(),
                        cf.controller().limit()),
                ambientSensor,
                hvacDevice,
                timeout);
    }

    private Thermostat createThermostat(String name, Double setpoint, RangeConfig rangeConfig, PidControllerConfig cf) {

        var range = map(Optional.ofNullable(rangeConfig).orElse(new RangeConfig(10.0, 40.0)));
        return new Thermostat(Clock.systemUTC(), name, range, setpoint, cf.p(), cf.i(), cf.d(), cf.limit());
    }

    private ZoneSettings map(ZoneSettingsConfig source) {

        return new ZoneSettings(
                Optional.ofNullable(source.enabled()).orElse(true),
                source.setpoint(),
                Optional.ofNullable(source.voting()).orElse(true),
                Optional.ofNullable(source.hold()).orElse(false),
                source.dumpPriority());
    }

    private Range<Double> map(RangeConfig cf) {
        return new Range<>(cf.min(), cf.max());
    }
}
