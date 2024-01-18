package net.sf.dz3r.runtime.config.quarkus.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public interface ScheduleConfig {
    @JsonProperty
    Set<CalendarConfigEntry> googleCalendar();
}
