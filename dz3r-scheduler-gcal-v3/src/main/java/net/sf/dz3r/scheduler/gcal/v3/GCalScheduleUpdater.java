package net.sf.dz3r.scheduler.gcal.v3;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.scheduler.SchedulePeriod;
import net.sf.dz3r.scheduler.SchedulePeriodFactory;
import net.sf.dz3r.scheduler.ScheduleUpdater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GCalScheduleUpdater implements ScheduleUpdater {

    protected final Logger logger = LogManager.getLogger();

    private static final String LITERAL_APP_NAME = "Home Climate Control-DZ-3.5";
    private static final String STORED_CREDENTIALS = ".dz/calendar";
    private static final String CLIENT_SECRETS = "/client_secrets.json";

    private static final DateTimeFormatter RFC3339DateTimeFormatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ssZZZZZ")
            .toFormatter();
    private static final SchedulePeriodFactory schedulePeriodFactory = new SchedulePeriodFactory();
    private static final SettingsParser settingsParser = new SettingsParser();

    private final Map<String, String> name2calendar;
    private final Duration pollInterval;

    /**
     * Create an instance with a poll interval of 1 minute.
     *
     * @param name2calendar The key is the zone name, the value is the calendar name for this zone's schedule.
     */
    public GCalScheduleUpdater(Map<String, String> name2calendar) {
        this(name2calendar, Duration.of(1, ChronoUnit.MINUTES));
    }

    /**
     * Create an instance.
     *
     * @param name2calendar The key is the zone name, the value is the calendar name for this zone's schedule.
     * @param pollInterval Schedule source poll interval.
     */
    public GCalScheduleUpdater(Map<String, String> name2calendar, Duration pollInterval) {
        this.name2calendar = name2calendar;
        this.pollInterval = pollInterval;
    }

    @Override
    public Flux<Map.Entry<String, SortedMap<SchedulePeriod, ZoneSettings>>> update() {

        logger.info("Starting updates every {}", pollInterval);

        return Flux
                .interval(Duration.ZERO, pollInterval)
                .doOnNext(v -> logger.debug("heartbeat: {}", v))
                .map(this::getCalendars)
                .flatMap(this::filterCalendars)

                // There will be very likely many zones which means there will be redundant I/O latency,
                // and even Raspberry Pi is multicore which means that parallelizing those latencies will yield
                // significant benefit

                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(this::getEvents)

                // This is still computationally expensive, but with a different breakdown; regroup (inside)
                .sequential()
                .map(this::convertEvents)
                .map(this::convertZoneName);
    }

    private Map.Entry<Calendar, List<CalendarListEntry>> getCalendars(Long ignore) {

        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new File(System.getProperty("user.home"), STORED_CREDENTIALS));

            Credential credential = authorize(httpTransport, jsonFactory, dataStoreFactory);

            // VT: NOTE: There's no need for us to do this more often than we really need.
            // I'd assume that once a year would be just fine. Maybe a bit too often, but come on, let's be realistic,
            // users need some extra fun.
            credential.setExpiresInSeconds(60L * 60L * 24L * 365L);

            var calendarClient = new Calendar.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName(LITERAL_APP_NAME)
                    .build();

            var list = calendarClient
                    .calendarList()
                    .list()
                    .execute()
                    .getItems();

            logger.debug("Calendar list: {} items", list.size());
            return new AbstractMap.SimpleEntry<>(calendarClient, list);

        } catch (Throwable t) {// NOSONAR Consequences have been considered

            logger.error("getClient() failed, returning empty list", t);
            return new AbstractMap.SimpleEntry<>(null, List.of());
        }
    }

    private Credential authorize(HttpTransport httpTransport, JsonFactory jsonFactory, FileDataStoreFactory dataStoreFactory) throws IOException {

        try (InputStream in = getClass().getResourceAsStream(CLIENT_SECRETS)) {

            if (in == null) {
                throw new IOException("null stream trying to open " + CLIENT_SECRETS);
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in, StandardCharsets.UTF_8));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport,
                    jsonFactory,
                    clientSecrets,
                    Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory).build();

            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        }
    }

    /**
     * Filter out calendars that are not mapped to zone schedules.
     *
     * @param source List of all calendars available.
     *
     * @return Flux of just zone schedule calendars.
     */
    private Flux<Map.Entry<Calendar, CalendarListEntry>> filterCalendars(Map.Entry<Calendar, List<CalendarListEntry>> source) {

        var zoneCount = new AtomicInteger();
        var calendarClient = source.getKey();

        try {

            return Flux.fromIterable(source.getValue())
                    .doOnNext(e -> logger.debug("Calendar found: {}", e.getSummary()))
                    .filter(e -> name2calendar.containsValue(e.getSummary()))
                    .doOnNext(e -> logger.debug("Zone schedule found: {}", e.getSummary()))
                    .doOnNext(e -> zoneCount.incrementAndGet())
                    .map(e -> new AbstractMap.SimpleEntry<>(calendarClient, e));

        } finally {
            logger.debug("Zone schedule list: {} items", zoneCount.get());
        }
    }

    private Flux<Map.Entry<CalendarListEntry, List<Event>>> getEvents(Map.Entry<Calendar, CalendarListEntry> source) {

        var calendarClient = source.getKey();
        var calendar = source.getValue();
        var calendarId = calendar.getId();

        Marker m = new Marker("getEvents(" + calendar.getSummary() + ")");
        try {

            var events = calendarClient.events().list(calendarId);

            // Let's grab three days of events in case there are events across midnight
            var now = ZonedDateTime.now();
            var min = now.minus(1, ChronoUnit.DAYS);
            var max = now.plus(1, ChronoUnit.DAYS);

            events.setTimeMin(new DateTime(min.format(RFC3339DateTimeFormatter)));
            events.setTimeMax(new DateTime(max.format(RFC3339DateTimeFormatter)));

            // Unroll recurring events into single and don't return parents; we don't need them
            events.setSingleEvents(true);

            logger.trace("Calendar API query: {}", events);

            var items = events.execute().getItems();

            logger.debug("{}: retrieved {} events", calendar.getSummary(), items.size());

            return Flux.just(new AbstractMap.SimpleEntry<>(calendar, items));

        } catch (IOException ex) {
            logger.error("Failed to retrieve events for {} (id={})", calendar, calendarId, ex);
            return Flux.empty();
        } finally {
            m.close();
        }
    }

    private Map.Entry<String, SortedMap<SchedulePeriod, ZoneSettings>> convertEvents(Map.Entry<CalendarListEntry, List<Event>> source) {

        var calendar = source.getKey();
        var schedule = new TreeMap<SchedulePeriod, ZoneSettings>();

        Flux.fromIterable(source.getValue())
                .flatMap(this::convertEvent)
                .subscribe(kv -> schedule.put(kv.getKey(), kv.getValue()));

        return new AbstractMap.SimpleEntry<>(calendar.getSummary(), schedule);
    }

    private Flux<Map.Entry<SchedulePeriod, ZoneSettings>> convertEvent(Event event) {
        ThreadContext.push("convertEvent");
        try {

            var period = parsePeriod(event);

            if (period == null) {
                // There must've been a log message about why
                return Flux.empty();
            }

            var settings = settingsParser.parse(event.getSummary().substring(period.name.length() + 1));

            return Flux.just(new AbstractMap.SimpleEntry<>(period, settings));

        } catch (Throwable t) { // NOSONAR Consequences have been considered

            logger.error("Failed to parse event, ignored: {}", event, t);
            return Flux.empty();

        } finally {
            ThreadContext.pop();
        }
    }

    private Map.Entry<String, SortedMap<SchedulePeriod, ZoneSettings>> convertZoneName(Map.Entry<String, SortedMap<SchedulePeriod, ZoneSettings>> source) {

        var calendarName = source.getKey();
        var zoneName = Flux.fromIterable(name2calendar.entrySet())
                .filter(kv -> kv.getValue().equals(calendarName))
                .map(Map.Entry::getKey)
                .blockFirst();

        return new AbstractMap.SimpleEntry<>(zoneName, source.getValue());
    }

    private SchedulePeriod parsePeriod(Event event) {
        ThreadContext.push("parsePeriod");

        try {

            EventDateTime start = event.getStart();
            EventDateTime end = event.getEnd();
            var today = isToday(start);
            String title = event.getSummary();

            int colonIndex = title.indexOf(':');

            if (colonIndex < 0) {
                throw new IllegalArgumentException("Can't parse period name out of event title '" + title + "' (must be separated by a colon)");
            }

            String periodName = title.substring(0, colonIndex).trim();

            logger.trace("name: '{}'", periodName);
            logger.trace("  {} to {}, today={}", start, end, today);

            if (!today) {
                return null;
            }

            if (isDateOnly(start)) {
                logger.trace("All day event '{}': {}/{}", periodName, start, end);
                return schedulePeriodFactory.build(event.getId(), periodName, LocalTime.MIN, LocalTime.MAX, parseDays(event));
            }

            return schedulePeriodFactory.build(event.getId(), periodName, start.getDateTime().toString(), end.getDateTime().toString(), parseDays(event));

        } finally {
            ThreadContext.pop();
        }
    }

    private boolean isToday(EventDateTime eventDateTime) {

        var googleDateTime = eventDateTime.getDateTime();

        if (googleDateTime != null) {

            var dateTime = ZonedDateTime.parse(googleDateTime.toString());
            var date = dateTime.toLocalDate();

            return date.equals(LocalDate.now());
        }

        return LocalDate.parse(eventDateTime.getDate().toString()).equals(LocalDate.now());
    }


    private String parseDays(Event source) {

        // At this time, setSingleEvents(true) makes recurrence to be always null, so it is just - "today".
        // Let's just make sure it stays this way.

        var r = source.getRecurrence();
        if (r != null) {
            logger.error("Lo and behold, recurrence is not null: {}", r);
        }

        var today = LocalDate.now().getDayOfWeek().getValue() - 1;

        final var days = "MTWTFSS";
        var result = new StringBuilder();

        for (var day = 0; day < 7; day++) {
            result.append(day == today ? days.charAt(today) : " ");
        }

        return result.toString();
    }

    /**
     * @return {@code true} if the object contains just the date, false otherwise.
     */
    private boolean isDateOnly(EventDateTime source) {

        ThreadContext.push("isDateOnly");

        try {

            if (source.getDate() != null && source.getDateTime() == null) {
                return true;
            }

            if (source.getDate() == null && source.getDateTime() != null) {
                return false;
            }

            logger.error("source: {}", source);
            logger.error("date:   {}", source.getDate());
            logger.error("time:   {}", source.getDateTime());

            throw new IllegalArgumentException("API must have changed, both Date and DateTime are returned, need to revise the code");

        } finally {
            ThreadContext.pop();
        }
    }
}
