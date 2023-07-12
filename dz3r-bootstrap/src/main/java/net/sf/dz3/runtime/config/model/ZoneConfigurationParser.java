package net.sf.dz3.runtime.config.model;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.device.actuator.economizer.EconomizerContext;
import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import net.sf.dz3r.model.Range;
import net.sf.dz3r.model.Thermostat;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;
import org.apache.commons.lang3.tuple.ImmutablePair;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ZoneConfigurationParser extends ConfigurationContextAware {

    public ZoneConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<ZoneConfig> source) {

        Flux
                .fromIterable(source)
                .map(this::createZone)
                .subscribe(kv -> context.zones.register(kv.getKey(), kv.getValue()));
    }

    private Map.Entry<String, Zone> createZone(ZoneConfig cf) {

        var ts = createThermostat(cf.name(), cf.settings().setpoint(), cf.settings().setpointRange(), cf.controller());
        var eco = createEconomizer(cf.economizer());
        var zone = new Zone(ts, map(cf.settings()), eco);

        return new ImmutablePair<>(cf.id(), zone);
    }

    private EconomizerContext<?> createEconomizer(EconomizerConfig cf) {

        if (cf == null) {
            return null;
        }

        return new EconomizerContext<>(
                new EconomizerSettings(
                        cf.mode(),
                        cf.keepHvacOn(),
                        cf.changeoverDelta(),
                        cf.targetTemperature()),
                getSensorBlocking(cf.ambientSensor()),
                getSwitch(cf.switchAddress()));
    }

    private Thermostat createThermostat(String name, Double setpoint, RangeConfig rangeConfig, PidControllerConfig cf) {

        var range = map(Optional.ofNullable(rangeConfig).orElse(new RangeConfig(10.0, 40.0)));
        return new Thermostat(name, range, setpoint, cf.p(), cf.i(), cf.d(), cf.limit());
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
