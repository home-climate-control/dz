package net.sf.dz3.runtime.config.protocol.onewire;

import net.sf.dz3.runtime.config.hardware.SensorConfig;
import net.sf.dz3.runtime.config.hardware.SwitchConfig;

import java.util.List;

/**
 * 1-Wire bus adapter descriptor.
 *
 * @param serialPort Serial port for the device bus. Mandatory.
 * @param sensors List of sensors, optional.
 * @param switches List of switches, optional.
 */
public record OnewireBusConfig(
        String serialPort,
        List<SensorConfig> sensors,
        List<SwitchConfig> switches) {
}
