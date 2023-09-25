package net.sf.dz3r.runtime.config.quarkus.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface CalendarConfigEntry {
    @JsonProperty
    String zone();
    @JsonProperty
    String calendar();
}
