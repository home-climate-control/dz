package net.sf.dz3.runtime.config.hardware;

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
