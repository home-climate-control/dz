package net.sf.dz3r.scheduler;

import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Listens to the schedule event feed coming from {@link ScheduleUpdater}, and
 * passes the commands down to {@link net.sf.dz3r.model.Zone}s.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class Scheduler {

    private final Logger logger = LogManager.getLogger();

    private final Clock clock;
    private final Duration scheduleGranularity;

    private final SchedulePeriodMatcher periodMatcher = new SchedulePeriodMatcher();

    private final Map<String, Zone> name2zone;
    private final Map<Zone, SortedMap<SchedulePeriod, ZoneSettings>> zone2schedule = new TreeMap<>();
    private final Map<Zone, SchedulePeriod> zone2period = new TreeMap<>();

    public Scheduler(Map<String, Zone> name2zone) {
        this(Clock.system(TimeZone.getDefault().toZoneId()), name2zone, Duration.of(10, ChronoUnit.SECONDS));
    }

    public Scheduler(Clock clock, Map<String, Zone> name2zone, Duration scheduleGranularity) {
        this.clock = clock;
        this.scheduleGranularity = scheduleGranularity;

        this.name2zone = name2zone;

        logger.info("Using {} time zone", clock.getZone());
        logger.info("Synchronizing schedule every {} seconds", scheduleGranularity.getSeconds());

        logger.info("{} zone[s] considered for scheduling:", name2zone.size());
        Flux.fromIterable(name2zone.entrySet())
                .subscribe(kv -> logger.info("  {}", kv.getKey()));
    }

    public Flux<Map.Entry<String, Map.Entry<SchedulePeriod, ZoneSettings>>> connect(Flux<Map.Entry<String, SortedMap<SchedulePeriod, ZoneSettings>>> source) {

        // Observe
        var observe = source
                .flatMap(this::updateSchedule)
                .flatMap(this::applySchedule)
                .doOnNext(s -> logger.info("scheduleObserve: {}", s))
                .subscribeOn(Schedulers.boundedElastic());

        // Execute
        var execute = Flux.interval(scheduleGranularity, Schedulers.boundedElastic())
                .flatMap(s -> Flux.fromIterable(zone2schedule.entrySet()))
                .flatMap(this::applySchedule)
                .doOnNext(s -> logger.info("scheduleExecute: {}", s))
                .subscribeOn(Schedulers.boundedElastic());

        return Flux
                .merge(observe, execute)
                .doOnNext(s -> logger.info("scheduleFlux: {}", s));
    }

    private Flux<Map.Entry<Zone, SortedMap<SchedulePeriod, ZoneSettings>>> updateSchedule(Map.Entry<String, SortedMap<SchedulePeriod, ZoneSettings>> source) {

        var zoneName = source.getKey();
        var schedule = source.getValue();
        var zone = name2zone.get(zoneName);

        if (zone == null) {
            logger.trace("scheduler {} unknown zone '{}', schedule ignored, known zones: {}", Integer.toHexString(hashCode()), zoneName, name2zone.keySet());
            return Flux.empty();
        }

        zone2schedule.put(zone, schedule);
        return Flux.just(new AbstractMap.SimpleEntry<>(zone, schedule));
    }

    private synchronized Flux<Map.Entry<String, Map.Entry<SchedulePeriod, ZoneSettings>>> applySchedule(Map.Entry<Zone, SortedMap<SchedulePeriod, ZoneSettings>> source) {

        ThreadContext.push("applySchedule");

        try {

            var zone = source.getKey();
            var zoneName = zone.getAddress();

            if (Boolean.TRUE.equals(zone.getSettings().hold)) {
                logger.debug("{}: on hold, left alone", zoneName);

                // This and below:
                // Whatever was displayed at the console previously, will stay.
                // May be problematic for HCC Remote and generally look weird, need to confirm that UX is right.
                return Flux.empty();
            }

            var schedule = source.getValue();

            logger.debug("{} schedule ({} entries)", zoneName, schedule.size());

            Flux.fromIterable(schedule.keySet())
                    .subscribe(s -> logger.debug("  {}", s));

            var now = LocalDateTime.now(clock);
            var period = periodMatcher.match(schedule, now);
            var currentPeriod = zone2period.get(zone);

            logger.debug("{}: matched time={} period={}", zoneName, now, period);

            if (same(currentPeriod, period)) {
                logger.debug("{}: already at {}", zoneName, period);
                return Flux.empty();
            }

            zone2period.put(zone, period);

            if (period != null) {
                var settings = source.getValue().get(period);
                logger.info("{}: settings applied: {}", zoneName, settings);
                zone.setSettings(settings);
                return Flux.just(new AbstractMap.SimpleEntry<>(zoneName, new AbstractMap.SimpleEntry<>(period, settings)));
            } else {
                logger.info("{}: no active period, settings left as they were", zoneName);
                return Flux.just(new AbstractMap.SimpleEntry<>(zoneName, null));
            }

        } finally {
            ThreadContext.pop();
        }
    }

    private boolean same(SchedulePeriod current, SchedulePeriod found) {

        if (current == null) {
            return found == null;
        }

        return current.equals(found);
    }
}
