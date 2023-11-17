package net.sf.dz3r.runtime.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Identifiable {
    @JsonProperty("id")
    String id();
}
