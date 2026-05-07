package net.sf.dz3r.runtime.config.protocol.onewire;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import net.sf.dz3r.runtime.config.hardware.SensorConfig;
import net.sf.dz3r.runtime.config.hardware.SwitchConfig;

import java.util.Set;

/**
 * 1-Wire bus adapter descriptor.
 *
 * @param serialPort Serial port for the device bus. Mandatory.
 * @param sensors List of sensors, optional.
 * @param switches List of switches, optional.
 */
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record OnewireBusConfig(
        String serialPort,
        Set<SensorConfig> sensors,
        Set<SwitchConfig> switches) {
}
