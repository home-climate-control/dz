package net.sf.dz3.scheduler.gcal;

import java.util.Date;
import java.util.Map;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.scheduler.AbstractScheduleUpdater;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxAware;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Base class for schedule updaters using Google Calendar as a back end.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2014
 */
public abstract class GCalScheduleUpdaterBase extends AbstractScheduleUpdater implements JmxAware {

    /**
     * Time of last known successful request to the calendar service, {@code 0} if none succeeded yet.
     */
    private long lastKnownGood = 0;
    
    protected final StatusParser statusParser = new StatusParser();

    public GCalScheduleUpdaterBase(Map<Thermostat, String> ts2source) {
        super(ts2source);
    }

    @JmxAttribute(description = "Last time when the schedule was actually extracted")
    public final String getLastKnownGood() {
        
        return new Date(lastKnownGood).toString();
    }

    @Override
    public final JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Google Calendar Schedule Updater",
                Integer.toHexString(hashCode()),
                "Pulls schedule from a Google Calendar");
    }

    /**
     * Update {@link #lastKnownGood} to current system time.
     */
    protected void touch() {
        lastKnownGood = System.currentTimeMillis();
    }
}
