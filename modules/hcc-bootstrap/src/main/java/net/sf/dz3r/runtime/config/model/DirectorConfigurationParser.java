package net.sf.dz3r.runtime.config.model;

import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.scheduler.ScheduleUpdater;
import net.sf.dz3r.view.Connector;
import net.sf.dz3r.view.MetricsCollector;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectorConfigurationParser extends ConfigurationContextAware {

    public DirectorConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<UnitDirectorConfig> source) {

        Flux
                .fromIterable(Optional.ofNullable(source).orElse(Set.of()))
                .map(this::parse)
                .subscribe(d -> context.directors.register(d.getAddress(), d));
    }

    private UnitDirector parse(UnitDirectorConfig cf) {

        // VT: FIXME: See if Flux.zip() provides any benefit here

        return new UnitDirector(
                cf.id(),
                getSchedule(),
                getCollectors(),
                getConnectors(),
                getSensorFeed2ZoneMapping(cf.sensorFeedMapping()),
                getUnitController(cf.unit()),
                getHvacDevice(cf.hvac()),
                cf.mode());
    }

    private ScheduleUpdater getSchedule() {
        return context
                .schedule
                .getFlux()
                .map(Map.Entry::getValue)
                .blockFirst();
    }

    private Set<MetricsCollector> getCollectors() {
        return context
                .collectors
                .getFlux()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet())
                .block();
    }

    private Set<Connector> getConnectors() {
        return context
                .connectors
                .getFlux()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet())
                .block();
    }
}
