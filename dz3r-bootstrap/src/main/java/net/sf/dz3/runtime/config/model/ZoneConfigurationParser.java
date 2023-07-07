package net.sf.dz3.runtime.config.model;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.device.actuator.economizer.EconomizerContext;
import net.sf.dz3r.model.Range;
import net.sf.dz3r.model.Thermostat;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;
import reactor.core.publisher.Flux;

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
                .doOnNext(z -> logger.info("zone: {}", z))
                .blockLast();
    }

    private Zone createZone(ZoneConfig cf) {

        var ts = createThermostat(cf.name(), cf.settings().setpoint(), cf.settings().setpointRange(), cf.controller());
        var eco = createEconomizer(cf.economizer());
        return new Zone(ts, map(cf.settings()), eco);
    }

    private EconomizerContext<?> createEconomizer(EconomizerConfig cf) {

        if (cf != null) {
            // VT: FIXME: Need sensor feeds at this point, don't have them exposed yet
            logger.error("FIXME: createEconomizer() not implemented for switch={}", cf.switchAddress());
        }

        return null;
    }

    private Thermostat createThermostat(String name, Double setpoint, RangeConfig rangeConfig, PidControllerConfig cf) {

        var range = map(Optional.ofNullable(rangeConfig).orElse(new RangeConfig(10.0, 40.0)));
        return new Thermostat(name, range, setpoint, cf.p(), cf.i(), cf.d(), cf.limit());
    }

    private ZoneSettings map(ZoneSettingsConfig source) {
        return new ZoneSettings(source.enabled(), source.setpoint(), source.voting(), source.hold(), source.dumpPriority());
    }

    private Range<Double> map(RangeConfig cf) {
        return new Range<>(cf.min(), cf.max());
    }
}
