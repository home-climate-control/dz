package net.sf.dz3.runtime.config.quarkus.protocol.onewire;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.dz3.runtime.config.quarkus.hardware.SensorConfig;
import net.sf.dz3.runtime.config.quarkus.hardware.SwitchConfig;

import java.util.Set;

/**
 * 1-Wire bus adapter descriptor.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface OnewireBusConfig {
    @JsonProperty("serial-port")
    String serialPort();
    @JsonProperty("sensors")
    Set<SensorConfig> sensors();
    @JsonProperty("switches")
    Set<SwitchConfig> switches();
}
