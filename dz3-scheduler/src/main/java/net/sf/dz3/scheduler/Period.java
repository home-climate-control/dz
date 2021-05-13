package net.sf.dz3.scheduler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.sf.dz3.device.model.ZoneStatus;

import org.apache.logging.log4j.ThreadContext;
import org.joda.time.DateTime;

/**
 * Defines a time period when a given {@link ZoneStatus} is to be activated.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2018
 */
public class Period implements Comparable<Period> {
    
    private static final String[] dateFormats = {
        
        // Formats are hungry, hence most complicated formats have to be at the beginning,
        // or simpler ones will kick in (for example, "2:15 PM" will be picked up by "HH:mm").
        
        "yy-MM-dd'T'hh:mm",
        "KK:mm aa",
        "hh:mm aa",
        "HH:mm",
        "HHmm",
    };
    
    private String daysOfWeek = "MTWTFSS";
    
    /**
     * Period name.
     * 
     * Has no significance other than for display and {@link #toString()}.
     */
    public final String name;

    /**
     * Start time offset against midnight, in milliseconds.
     */
    public final long start;
    
    /**
     * End time offset against midnight, in milliseconds.
     */
    public final long end;
    
    /**
     * Days when this period is scheduled to be active, as a bitmask.
     * 
     * 0x01 is Monday, 0x02 is Tuesday, and son on.
     */
    public final byte days;

    /**
     * Create an instance from human readable arguments.
     * 
     * @param name Period name.
     * @param startTime Start time in any reasonable format.
     * @param endTime End time in any reasonable format.
     * @param days String consisting of seven characters, any non-space character is treated as a bit set,
     * space is treated as a bit cleared. Recommended characters would be corresponding day names, Monday
     * in the first position (at offset 0).
     */
    public Period(String name, String startTime, String endTime, String days) {
        
        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("name can't be null or empty");
        }
        
        this.name = name;
        
        this.start = parseTime(startTime);
        this.end = parseTime(endTime);
        
        this.days = parseDays(days);
    }
    
    /**
     * Create a date object out of time offset against midnight.
     * 
     * This is necessary to dodge a time zone calculation that would be missing if a
     * {@code new Date(millis)} constructor was used.
     * 
     * @param offset Time offset against midnight, in milliseconds.
     * 
     * @return TOday's date with proper hours and minutes set.
     */
    private Date parseOffset(long offset) {
        
        Calendar cal = new GregorianCalendar();
        
        cal.set(Calendar.HOUR_OF_DAY, (int) (offset / 1000 / 60 / 60));
        cal.set(Calendar.MINUTE, (int)((offset % (1000 * 60 * 60)) / 1000 / 60));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        return cal.getTime();
    }

    @SuppressWarnings("deprecation")
    private long parseTime(String time) {
        
        ThreadContext.push("parse(" + time + ")");
        
        try {

            for (int offset = 0; offset < dateFormats.length; offset++) {

                // This is happening rarely enough so we can afford do go the long way,
                // in interest of better debugging
                String format = dateFormats[offset];
                DateFormat df = new SimpleDateFormat(format);

                try {

                    Date d = df.parse(time);

                    return d.getHours() * 1000 * 60 * 60 + d.getMinutes() * 1000 * 60;

                } catch (Throwable t) {

                    // VT: NOTE: Only uncomment this if you're having problems.
                    // This message may repeat over a hundred thousand times a day.
                    
                    // logger.debug("Failed to parse '" + time + "' as '" + format + "'");
                }
            }

            // This is bad, none of available formats worked

            StringBuilder sb = new StringBuilder();

            sb.append("Tried all available formats (");

            for (int offset = 0; offset < dateFormats.length; offset++) {

                sb.append('\'').append(dateFormats[offset]).append('\'');

                if (offset < dateFormats.length -1) {
                    sb.append(", ");
                }
            }

            sb.append(") to parse '").append(time).append("'and failed, giving up");

            throw new IllegalArgumentException(sb.toString());

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Convert the string given to the constructor into a bit mask.
     * 
     * @param days String to convert.
     * 
     * @return Bit mask to use as {@link #days}.
     */
    private byte parseDays(String days) {
        
        if (days == null || days.length() != 7) {
            
            throw new IllegalArgumentException("days argument malformed, see source code for instructions");
        }
        
        byte result = 0x00;
        
        for (int offset = 0; offset < 7; offset++) {
            
            byte mask = (byte)(0x01 << offset); 
            char c = days.charAt(offset);
            
            if (c != ' ') {
                
                result = (byte)(result | mask);
            }
        }

        return result;
    }
    
    /**
     * Check if the given time is within this period.
     * 
     * @param d Date (or, rather, time) to check for inclusion.
     * 
     * @return {@code true} if this period includes the time.
     */
    public boolean includes(DateTime d) {
        
        long offset = d.getHourOfDay() * 1000 * 60 * 60 + d.getMinuteOfHour() * 1000 * 60;
        
        return includes(offset);
    }

    /**
     * Check if the given offset belongs to this period.
     * 
     * @param offset Offset to check, in milliseconds.
     * 
     * @return {@code true} if the offset is within this period.
     */
    public boolean includes(long offset) {
        
        return start <= offset && offset <= end;
    }

    /**
     * Check if this period is active on the day of week of the given date.
     * 
     * @param d Date to check for inclusion.
     * 
     * @return {@code true} if this period includes is active for the date's day of week..
     */
    public boolean includesDay(DateTime d) {
        
        // Sunday based
        int day = d.getDayOfWeek();
        
        // Monday based
        day = (day + 6) % 7;
        
        return (days & (0x01 << day)) != 0;
    }
    
    @Override
    public int compareTo(Period o) {
        
        int diff = (int)(start - o.start);
        
        if (diff != 0) {
            
            return diff;
        }
        
        // If two events start at the same time, the one that ends later is considered "less",
        // to allow easy overrides
        
        return (int)(o.end - end); 
    }
    
    @Override
    public String toString() {
     
        final DateFormat df = new SimpleDateFormat("HH:mm"); 
        StringBuilder sb = new StringBuilder();
        
        sb.append(name).append(" (");
        sb.append(df.format(parseOffset(start)));
        sb.append(" to ");
        sb.append(df.format(parseOffset(end)));
        sb.append(" on ");
        
        for (int offset = 0; offset < 7; offset++) {
            
            int mask = 0x01 << offset;
            int set = days & mask;
            
            sb.append(set != 0 ? daysOfWeek.charAt(offset) : ".");
        }
        
        sb.append(")");
        
        return sb.toString();
    }
}
