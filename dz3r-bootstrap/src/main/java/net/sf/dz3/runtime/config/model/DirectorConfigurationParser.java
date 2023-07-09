package net.sf.dz3.runtime.config.model;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.model.UnitDirector;
import reactor.core.publisher.Flux;

import java.util.Set;

public class DirectorConfigurationParser extends ConfigurationContextAware {

    public DirectorConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<UnitDirectorConfig> source) {

        Flux
                .fromIterable(source)
                .map(this::parse)
                .subscribe(d -> context.directors.register(d.getAddress(), d));
    }

    private UnitDirector parse(UnitDirectorConfig cf) {

        logger.error("FIXME: UnitDirector({}) creation is TOTALLY WRONG", cf.id());

        // VT: FIXME: Dummy data except for the name to get the stub out and then return with necessary arguments

        return new UnitDirector(
                cf.id(),
                null,
                Set.of(),
                Set.of(),
                getSensorFeed2ZoneMapping(cf.sensorFeedMapping()),
                getUnitController(cf.unit()),
                getHvacDevice(cf.hvac()),
                cf.mode());
    }
}
