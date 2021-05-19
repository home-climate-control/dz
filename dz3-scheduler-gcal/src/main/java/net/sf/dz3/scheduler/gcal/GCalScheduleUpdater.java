package net.sf.dz3.scheduler.gcal;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.scheduler.AbstractScheduleUpdater;
import net.sf.dz3.scheduler.Period;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.google.gdata.client.ClientLoginAccountType;
import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.TextConstruct;
import com.google.gdata.data.calendar.CalendarEntry;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.calendar.CalendarFeed;
import com.google.gdata.data.extensions.When;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

/**
 * Schedule updater using Google Calendar as a back end.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2011
 */
public class GCalScheduleUpdater extends AbstractScheduleUpdater implements JmxAware {

    private final Logger logger = Logger.getLogger(getClass());
    
    private final String LITERAL_APP_NAME = "Home Climate Control-DZ-3.5";
    
    private final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final NumberFormat tzFormatter = new DecimalFormat("+#00;-#00");
    private final StatusParser statusParser = new StatusParser();
    
    private String username;
    private String password;
    private String domain;
    
    private long lastKnownGood = 0;
    
    /**
     * Cached instance of a Google calendar service.
     */
    private CalendarService calendarService;
    
    /**
     * Create an instance with a Google username, but without a password.
     * 
     * Password can be later {@link #setUsername(String) provided via JMX},
     * as well as {@link #setDomain(String) domain}.
     * 
     * @param ts2source Keys are thermostats to update the schedule for,
     * values are calendar names to pull schedules from.
     * @param username Google (or Google Apps) account name.
     */
    public GCalScheduleUpdater(Map<Thermostat, String> ts2source, String username) {
        
        this(ts2source, username, null, null);
    }

    /**
     * Create an instance with username and domain information, but without a password.
     * 
     * Password can be later {@link #setUsername(String) provided via JMX}.
     * 
     * @param ts2source Keys are thermostats to update the schedule for,
     * values are calendar names to pull schedules from.
     * @param username Google (or Google Apps) account name.
     * @param domain Google Apps domain. If {@code null} or empty, Google will be used.
     */
    public GCalScheduleUpdater(Map<Thermostat, String> ts2source, String username, String domain) {
        
        this(ts2source, username, null, domain);
    }

    /**
     * Create an instance with authentication and domain information.
     * 
     * @param ts2source Keys are thermostats to update the schedule for,
     * values are calendar names to pull schedules from.
     * @param username Google (or Google Apps) account name.
     * @param password  Password for that account.
     * @param domain Google Apps domain. If {@code null} or empty, Google will be used.
     */
    public GCalScheduleUpdater(Map<Thermostat, String> ts2source, String username, String password, String domain) {

        super(ts2source);

        this.username = username;
        this.password = password;
        this.domain = domain;
    }
    
    @JmxAttribute(description = "username")
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    @JmxAttribute(description = "password")
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    @JmxAttribute(description = "domain")
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public Map<Thermostat, SortedMap<Period, ZoneStatus>> update() throws IOException {

        NDC.push("update");
        
        long start = System.currentTimeMillis();
        
        Map<Thermostat, SortedMap<Period, ZoneStatus>> ts2schedule = new TreeMap<Thermostat, SortedMap<Period, ZoneStatus>>();

        try {
            
            CalendarService cal = createService();
            URL feedUrl = new URL("https://www.google.com/calendar/feeds/default/allcalendars/full");
            CalendarFeed calendarFeed = cal.getFeed(feedUrl, CalendarFeed.class);
            
            List<CalendarEntry> calendars = calendarFeed.getEntries();
            logger.debug("Found " + calendars.size() + " calendars");
            logger.debug("Elapsed time: listCalendars: " + (System.currentTimeMillis() - start) + "ms");
            
            for (Iterator<CalendarEntry> i = calendars.iterator(); i.hasNext(); ) {
                
                CalendarEntry calendar = i.next();
                TextConstruct titleConstruct = calendar.getTitle();
                
                if (titleConstruct == null) {
                
                    logger.warn("Failed to retrieve title for " + calendar + ", skipping");
                    continue;
                }
                
                String title = titleConstruct.getPlainText();
                
                logger.debug("Calendar: " + title);
                
                
                if (title != null) {
                    
                    updateCalendar(ts2schedule, cal, calendar);
                }
            }
            
        } catch (AuthenticationException ex) {
            
            // Need to reset the instance so it will be regenerated next time,
            // it is possible that the authenticated session has expired
            
            calendarService = null;
            
            throw new IOException("Authentication failed", ex);
            
        } catch (ServiceException ex) {

            throw new IOException("Failed to retrieve the feed", ex);
            
        } finally {
            logger.debug("Elapsed time: update: " + (System.currentTimeMillis() - start) + "ms");
            NDC.pop();
        }
        
        return ts2schedule;
    }
    
    /**
     * Pull period data from an individual calendar.
     * 
     * @param ts2schedule Map to put found periods into.
     * @param cal Calendar service to use.
     * @param calendar Individual calendar to pull period data from.
     */
    private void updateCalendar(Map<Thermostat, SortedMap<Period, ZoneStatus>> ts2schedule, CalendarService cal, CalendarEntry calendar) {

        NDC.push("updateCalendar");
        long startMillis = System.currentTimeMillis();
        SortedMap<Period, ZoneStatus> period2status = new TreeMap<Period, ZoneStatus>();
        
        try {
            
            String name = calendar.getTitle().getPlainText();
            
            Set<Thermostat> tSet = getByName(name);
            
            if (tSet == null || tSet.isEmpty()) {
                
                logger.debug("No zone '" + name + "' configured, skipped");
                return;
            }

            logger.info("Zone name: " + name);
            
            String id = getCalendarId(calendar);

            logger.info(calendar.getTitle().getPlainText() + " id=" + id);

            URL feedUrl = new URL("https://www.google.com/calendar/feeds/" + id + "/private/full-noattendees");
          
            CalendarQuery q = new CalendarQuery(feedUrl);
            
            Calendar tempCalendar = new GregorianCalendar();
            
            tempCalendar.set(Calendar.HOUR_OF_DAY, 0);
            tempCalendar.set(Calendar.MINUTE, 0);
            tempCalendar.set(Calendar.SECOND, 0);
            tempCalendar.set(Calendar.MILLISECOND, 0);
            
            Date start = tempCalendar.getTime();

            tempCalendar.set(Calendar.HOUR_OF_DAY, 23);
            tempCalendar.set(Calendar.MINUTE, 59);
            tempCalendar.set(Calendar.SECOND, 59);
            tempCalendar.set(Calendar.MILLISECOND, 0);
            
            Date end = tempCalendar.getTime();
            
            // Now, let's produce the time zone offset and shove it down the throat of
            // Google's invention that isn't capable of parsing normal TZ representation.
            // This will not work with fractional offsets, but you know what?
            // Go fix it yourself if you live in one of those time zones.
            
            int tzShift = tempCalendar.getTimeZone().getRawOffset() / (60000 * 60);
            String tzTail = tzFormatter.format(tzShift) + ":00";
            
            DateTime dtStart = DateTime.parseDateTime(dateFormatter.format(start) + tzTail);
            DateTime dtEnd = DateTime.parseDateTime(dateFormatter.format(end) + tzTail);
            
            logger.debug("Day span: " + dtStart + " ... " + dtEnd);
            
            q.setMinimumStartTime(dtStart);
            q.setMaximumStartTime(dtEnd);
            
            CalendarEventFeed eventFeed = cal.query(q, CalendarEventFeed.class);

            // cal.query() has been known to get stuck, let's update the timestamp
            lastKnownGood = System.currentTimeMillis();            

            for (int i = 0; i < eventFeed.getEntries().size(); i++) {
                
                CalendarEventEntry event = eventFeed.getEntries().get(i);
                
                updateEvent(period2status, event);
            }
            
            for (Iterator<Thermostat> i = tSet.iterator(); i.hasNext(); ) {

                ts2schedule.put(i.next(), period2status);
            }
            
        } catch (Throwable t) {
            
            logger.error("Failed to pull schedule for " + calendar.getTitle().getPlainText(), t);

        } finally {

            logger.debug("Elapsed time: updateCalendar: " + (System.currentTimeMillis() - startMillis) + "ms");
            NDC.pop();
        }
    }

    /**
     * Update {@code period2status} with period information taken from the {@code event}.
     * 
     * @param period2status Map to put period information into.
     * @param event Data source.
     */
    private void updateEvent(SortedMap<Period,ZoneStatus> period2status, CalendarEventEntry event) {

        NDC.push("updateEvent");
        
        try {
            
            List<When> times = event.getTimes();
            String title = event.getTitle().getPlainText();
            
            logger.info(title + ": " + times.size() + " times");
            
            if (!times.isEmpty()) {

                for (Iterator<When> i2 = times.iterator(); i2.hasNext(); ) {

                    When when = i2.next();
                    DateTime start = when.getStartTime();
                    DateTime end = when.getEndTime();
                    
                    logger.info("  " + start + " to " + end);
                    
                    // VT: FIXME: Need to pass recurrence as well,
                    // for cases of not connected for over a day
                    
                    parseEvent(period2status, title, start, end);
                }
            }

        } finally {
            NDC.pop();
        }
    }

    private void parseEvent(SortedMap<Period,ZoneStatus> period2status, String title, DateTime start, DateTime end) {
        
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

    private Period parsePeriod(String periodName, DateTime start, DateTime end) {
        
        NDC.push("parsePeriod");
        
        try {
            
            String startTime;
            String endTime;
          
            if (start.isDateOnly()) {

                logger.debug("All day event: " + start + "/" + end);
                
                startTime = start.toString() + "T00:00:00";
                endTime = start.toString() + "T23:59:59";
                
                // Check if this is today
                
                try {
                    
                    // VT: NOTE: This mess could've been avoided if Period had real time,
                    // not just offset from midnight. Need to keep this in mind if
                    // this piece of code ever gets refactored.

                    Calendar cal = new GregorianCalendar();
                    Date d = dateFormatter.parse(startTime);
                    int today = cal.get(Calendar.DAY_OF_YEAR);
                    
                    cal.setTime(d);
                    int startDay = cal.get(Calendar.DAY_OF_YEAR);
                    
                    logger.debug("Today vs. start day: " + today + "/" + startDay);
                    
                    if (today != startDay) {
                        
                        // Nope, this event is not for today
                        return null;
                    }
                
                } catch (ParseException ex) {
                    
                    throw new IllegalStateException("Failed to parse " + startTime);
                }
                

            } else {

                // Adjust the time zone to the local system time zone
                // (they may differ, and the only place where this is visible is calendar settings)
                
                Calendar calendar = new GregorianCalendar();
                
                int tzShift = calendar.getTimeZone().getRawOffset() / 60000;
                
                // This will not change the time, just the representation
                start.setTzShift(tzShift);
                end.setTzShift(tzShift);

                logger.debug("Local time zone start/end: " + start + "/" + end);
                
                // Kinda ugly, but efficient
                startTime = start.toString().substring(11, 16);
                endTime = end.toString().substring(11, 16);
            }

            // VT: NOTE: Recurrence is irrelevant ere since he schedule
            // is  dynamically regenerated - but it'd be nice to make it right later
            // (though what';s the point other than a scenario when the system is offline for over a day?)
            
            return new Period(periodName, startTime, endTime, ".......");
        
        } finally {
            NDC.pop();
        }
    }

    private String getCalendarId(CalendarEntry calendar) {

        String id = calendar.getId();
        
        // VT: NOTE: This is ugly, but I can't find a more elegant way of doing it at the moment
        final String prefix = "https://www.google.com/calendar/feeds/default/calendars/";
        
        if (!id.startsWith(prefix)) {
            
            throw new IllegalStateException("Don't know how to handle calendar ID: " + id);
        }
        
        return id.substring(prefix.length());
    }

    /**
     * Create the service and authenticate it.
     * 
     * @return Authenticated calendar service.
     * @throws AuthenticationException if authentication failed.
     */
    private CalendarService createService() throws AuthenticationException {
        
        NDC.push("createService");
        long start = System.currentTimeMillis();
        
        try {
            
            if (calendarService != null) {
            
                logger.debug("Returning cached instance");
                return calendarService;
            }

            logger.debug("Creating new instance");
            
            calendarService =  new CalendarService(LITERAL_APP_NAME);

            if (username == null || "".equals(username) || password == null || "".equals(password)) {

                logger.warn("No authentication credentials provided, skipping authentication");

            } else {
                
                if (domain == null || "".equals(domain)) {
                    
                    logger.info("Using Google authentication");
                    calendarService.setUserCredentials(username, password);

                } else {

                    String email = username + "@" + domain;
                    logger.info("Using Google App Engine authentication (" + email + ")");
                    calendarService.setUserCredentials(email, password, ClientLoginAccountType.HOSTED);
                }
            }
            
            return calendarService;
            
        } catch (AuthenticationException ex) {
            
            // Damn! Credentials must be wrong, but the instance already exists. Need to reset.
            calendarService = null;
            
            // And rethrow. Next time the updater runs, the instance will be recreated
            // (if things go right, that is).
            throw ex;
            
        } finally {
            
            logger.debug("Elapsed time: createService: " + (System.currentTimeMillis() - start) + "ms");
            
            NDC.pop();
        }
    }
    
    @JmxAttribute(description = "Last time when the schedule was actually extracted")
    public String getLastKnownGood() {
        
        return new Date(lastKnownGood).toString();
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Google Calendar Schedule Updater",
                username + (domain == null || "".equals(domain) ? "" : "@" + domain),
                "Pulls schedule from a Google Calendar");
    }
}
