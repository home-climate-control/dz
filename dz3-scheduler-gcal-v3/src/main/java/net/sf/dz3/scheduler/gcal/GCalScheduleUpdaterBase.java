package net.sf.dz3.scheduler.gcal;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.scheduler.AbstractScheduleUpdater;

import java.util.Date;
import java.util.Map;

/**
 * Base class for schedule updaters using Google Calendar as a back end.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class GCalScheduleUpdaterBase extends AbstractScheduleUpdater implements JmxAware {

    /**
     * Time of last known successful request to the calendar service, {@code 0} if none succeeded yet.
     */
    private long lastKnownGood = 0;

    protected final StatusParser statusParser = new StatusParser();

    protected GCalScheduleUpdaterBase(Map<Thermostat, String> ts2source) {
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
