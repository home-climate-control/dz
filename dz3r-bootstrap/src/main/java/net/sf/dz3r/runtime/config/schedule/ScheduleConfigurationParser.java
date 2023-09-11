package net.sf.dz3r.runtime.config.schedule;

import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.scheduler.ScheduleUpdater;
import net.sf.dz3r.scheduler.gcal.v3.GCalScheduleUpdater;
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

        var mapping = Flux
                .fromIterable(source)
                // This will not be fast
                .publishOn(Schedulers.boundedElastic())
                .flatMap(kv -> {
                    var zone = context.zones.getById("schedule.google-calendar", kv.zone());

                    return zone != null
                            ? Flux.just(new CalendarConfigEntry(zone.getAddress(), kv.calendar()))
                            : Flux.empty();
                })
                .collectMap(CalendarConfigEntry::zone, CalendarConfigEntry::calendar)
                .block();

        if (mapping.isEmpty()) {
            logger.warn("schedule.google-calendar: no mappable zones found, not creating the updater");
            return;
        }

        Flux.just(mapping)
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
