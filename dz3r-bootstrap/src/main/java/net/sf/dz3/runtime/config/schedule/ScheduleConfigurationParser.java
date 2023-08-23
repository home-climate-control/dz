package net.sf.dz3.runtime.config.schedule;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
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

        Flux
                .fromIterable(source)
                // This will not be fast
                .publishOn(Schedulers.boundedElastic())
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
