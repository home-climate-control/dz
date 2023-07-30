package net.sf.dz3r.signal.health;

import java.util.Map;

/**
 * Container for all known entities capable of reporting their health.
 *
 * In all the maps below, the key is the entity configuration ID, and the value is the last reported entity status.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public record SystemStatus(
        Map<String, SensorStatus> sensors,
        Map<String, SwitchStatus> switches,
        Map<String, HvacDeviceStatus> hvacDevices,
        Map<String, ConnectorStatus> connectors,
        Map<String, ConnectorStatus> collectors
) {
}
