package net.sf.dz3.scheduler.gcal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3.scheduler.Period;

import org.apache.log4j.NDC;

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
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

/**
 * Schedule updater using Google Calendar V3 as a back end.
 * 
 * Look for V3 API docs starting at https://developers.google.com/google-apps/calendar/ - but as time goes, it'll slip, there's no
 * specific URL for V3 docs while it is the last version.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2014
 */
public final class GCalScheduleUpdater extends GCalScheduleUpdaterBase {

    private static final String LITERAL_APP_NAME = "Home Climate Control-DZ-3.5";
    private static final String STORED_CREDENTIALS = ".dz/calendar";
    private static final String CLIENT_SECRETS = "/client_secrets.json"; 

    private static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss";
    private static final NumberFormat tzFormatter = new DecimalFormat("+#00;-#00");

    /**
     * Create an instance.
     * 
     * @param ts2source Keys are thermostats to update the schedule for, values are calendar names to pull schedules from.
     */
    public GCalScheduleUpdater(Map<Thermostat, String> ts2source) {
        super(ts2source);
    }

    @Override
    public Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException {

        NDC.push("update");
        Marker m = new Marker("update");
        
        Map<Thermostat, SortedMap<Period, ZoneStatus>> ts2schedule = new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();

        try {
            
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new File(System.getProperty("user.home"), STORED_CREDENTIALS));

            m.checkpoint("instantiated tools");

            Credential credential = authorize(httpTransport, jsonFactory, dataStoreFactory);
            
            m.checkpoint("authorized");

            // VT: NOTE: There's no need for us to do this more often than we really need.
            // I'd assume that once a year would be just fine. Maybe a bit too often, but come on, let's be realistic,
            // users need some extra fun.
            credential.setExpiresInSeconds(60L * 60L * 24L * 365L);
            
            Calendar calendarClient = new Calendar.Builder(httpTransport, jsonFactory, credential).setApplicationName(LITERAL_APP_NAME).build();
            
            m.checkpoint("instantiated client");

            CalendarList feed = calendarClient.calendarList().list().execute();
            
            List<CalendarListEntry> calendars = feed.getItems(); 
                    
            m.checkpoint("retrieved feed");

            logger.info(calendars.size() + " calendars found:");
            
            for (Iterator<CalendarListEntry> i = calendars.iterator(); i.hasNext(); ) {
                
                CalendarListEntry c = i.next();
                
                logger.info("  calendar: " + c.getSummary());
            }
            
            m.checkpoint("retrieved summary");

            for (Iterator<CalendarListEntry> i = calendars.iterator(); i.hasNext(); ) {
                
                CalendarListEntry calendar = i.next();
                
                updateCalendar(ts2schedule, calendarClient, calendar);
            }

            NDC.push("schedule");
            
            for (Iterator<Entry<Thermostat, SortedMap<Period, ZoneStatus>>> i = ts2schedule.entrySet().iterator(); i.hasNext(); ) {
                
                Entry<Thermostat, SortedMap<Period, ZoneStatus>> pair = i.next();
                
                logger.info(pair.getKey().getName() + ": " + pair.getValue().size() + " entries");
                
                for (Iterator<Entry<Period, ZoneStatus>> i2 = pair.getValue().entrySet().iterator(); i2.hasNext(); ) {
                    
                    logger.info("  " + i2.next());
                }
            }
            
            NDC.pop();
            
            return ts2schedule;
            
        } catch (GeneralSecurityException ex) {

            throw new IllegalStateException("Oops", ex);
            
        } finally {
            
            m.close();
            NDC.pop();
        }
    }

    private void updateCalendar(Map<Thermostat, SortedMap<Period, ZoneStatus>> ts2schedule, Calendar calendarClient, CalendarListEntry calendar) {
        
        NDC.push("updateCalendar");
        Marker m = new Marker("updateCalendar");
        
        try {

            String name = calendar.getSummary();
            
            Set<Thermostat> tSet = getByName(name);

            if (tSet == null || tSet.isEmpty()) {
                
                logger.debug("No zone '" + name + "' configured, skipped");
                return;
            }

            logger.info("Zone name: " + name);
            
            String id = calendar.getId();
            
            logger.info("id: " + id);
            
            parseEvents(ts2schedule, tSet, calendarClient, id);
            
        } catch (IOException ex) {

            logger.error("Unable to retrieve schedule for '" + calendar.getSummary() + "'", ex);

        } finally {
            
            m.close();
            NDC.pop();
        }

    }

    private void parseEvents(Map<Thermostat, SortedMap<Period, ZoneStatus>> ts2schedule, Set<Thermostat> tSet, Calendar calendarClient, String id) throws IOException {

        NDC.push("parseEvents");
        Marker m = new Marker("parseEvents");

        try {

            com.google.api.services.calendar.Calendar.Events.List events = calendarClient.events().list(id);
            
            java.util.Calendar tempCalendar = new GregorianCalendar();
            
            tempCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
            tempCalendar.set(java.util.Calendar.MINUTE, 0);
            tempCalendar.set(java.util.Calendar.SECOND, 0);
            tempCalendar.set(java.util.Calendar.MILLISECOND, 0);
            
            Date start = tempCalendar.getTime();

            tempCalendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
            tempCalendar.set(java.util.Calendar.MINUTE, 59);
            tempCalendar.set(java.util.Calendar.SECOND, 59);
            tempCalendar.set(java.util.Calendar.MILLISECOND, 0);
            
            Date end = tempCalendar.getTime();
            
            // Now, let's produce the time zone offset and shove it down the throat of
            // Google's invention that isn't capable of parsing normal TZ representation.
            // This will not work with fractional offsets, but you know what?
            // Go fix it yourself if you live in one of those time zones.
            
            int tzShift = tempCalendar.getTimeZone().getRawOffset() / (60000 * 60);
            String tzTail = tzFormatter.format(tzShift) + ":00";
            
            DateFormat dateFormatter = new SimpleDateFormat(dateFormat);
            DateTime dtStart = DateTime.parseRfc3339(dateFormatter.format(start) + tzTail);
            DateTime dtEnd = DateTime.parseRfc3339(dateFormatter.format(end) + tzTail);
            
            events.setTimeMin(dtStart);
            events.setTimeMax(dtEnd);
            events.setSingleEvents(true);
            
            logger.info("query: " + events);
            
            Events feed = events.execute();
            
            // cal.query() has been known to get stuck, let's update the timestamp
            touch();            

            parse(ts2schedule, tSet, feed.getItems());

        } finally {

            m.close();
            NDC.pop();
        }

    }

    private void parse(Map<Thermostat, SortedMap<Period, ZoneStatus>> ts2schedule, Set<Thermostat> tSet, List<Event> events) {

        logger.info(events.size() + " events found");

        SortedMap<Period, ZoneStatus> period2status = new TreeMap<Period, ZoneStatus>();

        for (Iterator<Event> i = events.iterator(); i.hasNext();) {
            
            updateEvent(period2status, i.next());
        }

        for (Iterator<Thermostat> i = tSet.iterator(); i.hasNext(); ) {

            ts2schedule.put(i.next(), period2status);
        }
    }

    /**
     * Update {@code period2status} with period information taken from the {@code event}.
     * 
     * @param period2status Map to put period information into.
     * @param event Data source.
     */
    private void updateEvent(SortedMap<Period,ZoneStatus> period2status, Event event) {

        NDC.push("updateEvent");
        
        try {
            
            EventDateTime start = event.getStart();
            EventDateTime end = event.getEnd();

            logger.info("  " + start + " to " + end);

            // VT: FIXME: Need to pass recurrence as well,
            // for cases of not connected for over a day

            parseEvent(period2status, event.getSummary(), start, end);

        } finally {
            NDC.pop();
        }
    }

    private void parseEvent(SortedMap<Period,ZoneStatus> period2status, String title, EventDateTime start, EventDateTime end) {
        
        NDC.push("parsePeriod");
        
        try {

            int colonIndex = title.indexOf(':');

            if (colonIndex < 0) {

                throw new IllegalArgumentException("Can't parse period name out of event title '" + title + "' (must be separated by a colon)");
            }

            String periodName = title.substring(0, colonIndex).trim();
            
            logger.info("name: '" + periodName + "'");
            
            Period p = parsePeriod(periodName, start, end);
            
            if (p == null) {
            
                logger.debug("Not today, skipping");
                return;
            }
            
            ZoneStatus status = statusParser.parse(title.substring(colonIndex + 1));
            
            logger.debug("Period: " + p);
            logger.debug("Status: " + status);
            
            period2status.put(p, status);
            
        } finally {
            NDC.pop();
        }
        
    }

    private Period parsePeriod(String periodName, EventDateTime start, EventDateTime end) {
        
        NDC.push("parsePeriod");
        
        try {
            
            String startTime;
            String endTime;
            
            if (isDateOnly(start)) {

                logger.debug("All day event: " + start + "/" + end);
                
                startTime = start.getDate().toString() + "T00:00:00";
                endTime = start.getDate().toString() + "T23:59:59";
                
                // Check if this is today
                
                try {
                    
                    // VT: NOTE: This mess could've been avoided if Period had real time,
                    // not just offset from midnight. Need to keep this in mind if
                    // this piece of code ever gets refactored.

                    GregorianCalendar cal = new GregorianCalendar();
                    Date d = new SimpleDateFormat(dateFormat).parse(startTime);
                    int today = cal.get(GregorianCalendar.DAY_OF_YEAR);
                    
                    cal.setTime(d);
                    int startDay = cal.get(GregorianCalendar.DAY_OF_YEAR);
                    
                    logger.debug("Today vs. start day: " + today + "/" + startDay);
                    
                    if (today != startDay) {
                        
                        // Nope, this event is not for today
                        return null;
                    }
                
                } catch (ParseException ex) {
                    
                    throw new IllegalStateException("Failed to parse " + startTime, ex);
                }
                

            } else {

                // logger.debug("Local time zone start/end: " + start + "/" + end);
                
                // Kinda ugly, but efficient
                startTime = start.getDateTime().toString().substring(11, 16);
                endTime = end.getDateTime().toString().substring(11, 16);

                logger.debug("Local time zone start/end: " + startTime + "/" + endTime);
            }

            return new Period(periodName, startTime, endTime, ".......");
        
        } finally {
            NDC.pop();
        }
    }

    /**
     * @return {@code true} if the object contains just the date, false otherwise.
     */
    private boolean isDateOnly(EventDateTime source) {
        
        NDC.push("isDateOnly");
        
        try {

            if (source.getDate() != null && source.getDateTime() == null) {
                return true;
            }
    
            if (source.getDate() == null && source.getDateTime() != null) {
                return false;
            }
            
            logger.error("source: " + source);
            logger.error("date:   " + source.getDate());
            logger.error("time:   " + source.getDateTime());
            
            throw new IllegalArgumentException("API must have changed, both Date and DateTime are returned, need to revise the code");

        } finally {
            NDC.pop();
        }
    }

    private Credential authorize(HttpTransport httpTransport, JsonFactory jsonFactory, FileDataStoreFactory dataStoreFactory) throws IOException {
        
        NDC.push("authorize");
        Marker m = new Marker("authorize");
        
        try {
            
            InputStream in = getClass().getResourceAsStream(CLIENT_SECRETS);
            
            if (in == null) {
                throw new IOException("null stream trying to open " + CLIENT_SECRETS);
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport,
                    jsonFactory,
                    clientSecrets,
                    Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory).build();

            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        } finally {
            
            m.close();
            NDC.pop();
        }
    }
}
