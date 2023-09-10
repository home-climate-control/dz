package net.sf.dz3r.runtime.config.schedule;

import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.scheduler.ScheduleUpdater;
import net.sf.dz3r.scheduler.gcal.v3.GCalScheduleUpdater;
import org.apache.commons.lang3.tuple.ImmutablePair;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;

public class ScheduleConfigurationParser extends ConfigurationContextAware {
    public ScheduleConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(ScheduleConfig cf) {

        if (cf == null || cf.googleCalendar() == null) {
            logger.warn("No calendar integration, proceeding anyway");
            return;
        }

        parseGCal(cf.googleCalendar());
    }

    private void parseGCal(Set<CalendarConfigEntry> source) {

        if (source == null || source.isEmpty()) {

            logger.warn("schedule is null or empty, skipped: {}", source);
            return;
        }

        Flux
                .fromIterable(source)
                // This will not be fast
                .publishOn(Schedulers.boundedElastic())
                .flatMap(kv -> {
                    var zoneId = kv.zone();
                    var zoneMapping = context.zones.getFlux();
                    var zoneEntry = zoneMapping
                            .filter(z -> z.getKey().equals(zoneId))
                            .blockFirst();

                    logger.debug("zone id={} entry={}", zoneId, zoneEntry);

                    if (zoneEntry != null) {
                        return Flux.just(new CalendarConfigEntry(zoneEntry.getValue().getAddress(), kv.calendar()));
                    }

                    logger.error("\"{}\" not found among configured zone IDs; existing mappings follow, you need the ID:", zoneId);
                    zoneMapping
                            .map(kv2 -> new ImmutablePair<>(kv2.getKey(), kv2.getValue().getAddress()))
                            .subscribe(entry -> logger.error("  id={}, name={}", entry.getKey(), entry.getValue()));

                    logger.error("{}: skipping to proceed with the rest of the configuration", zoneId);

                    return Flux.empty();
                })
                .collectMap(CalendarConfigEntry::zone, CalendarConfigEntry::calendar)
                .map(this::createUpdater)
                .subscribe(this::register);
    }


    private ScheduleUpdater createUpdater(Map<String, String> mapping) {
        return new GCalScheduleUpdater(mapping);
    }

    private void register(ScheduleUpdater u) {
        context.schedule.register("google-calendar", u);
    }
}
