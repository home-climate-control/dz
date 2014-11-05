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

    private String username;
    private String password;
    private String domain;

    /**
     * Time of last known successful request to the calendar service, {@code 0} if none succeeded yet.
     */
    private long lastKnownGood = 0;
    
    protected final StatusParser statusParser = new StatusParser();

    public GCalScheduleUpdaterBase(Map<Thermostat, String> ts2source, String username, String password, String domain) {
        super(ts2source);
    }

    @JmxAttribute(description = "username")
    public final String getUsername() {
        return username;
    }
    
    public final void setUsername(String username) {
        this.username = username;
    }

    @JmxAttribute(description = "password")
    public final String getPassword() {
        return password;
    }
    
    public final void setPassword(String password) {
        this.password = password;
    }

    @JmxAttribute(description = "domain")
    public final String getDomain() {
        return domain;
    }
    
    public final void setDomain(String domain) {
        this.domain = domain;
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
                username + (domain == null || "".equals(domain) ? "" : "@" + domain),
                "Pulls schedule from a Google Calendar");
    }

    /**
     * Update {@link #lastKnownGood} to current system time.
     */
    protected void touch() {
        lastKnownGood = System.currentTimeMillis();
    }
}
