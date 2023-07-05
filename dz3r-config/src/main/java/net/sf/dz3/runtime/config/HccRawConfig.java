package net.sf.dz3.runtime.config;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sf.dz3.runtime.config.connector.ConnectorConfig;
import net.sf.dz3.runtime.config.filter.FilterConfig;
import net.sf.dz3.runtime.config.hardware.HvacDeviceConfig;
import net.sf.dz3.runtime.config.hardware.MockConfig;
import net.sf.dz3.runtime.config.hardware.UnitControllerConfig;
import net.sf.dz3.runtime.config.model.ConsoleConfig;
import net.sf.dz3.runtime.config.model.UnitDirectorConfig;
import net.sf.dz3.runtime.config.model.WebUiConfig;
import net.sf.dz3.runtime.config.model.ZoneConfig;
import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.protocol.onewire.OnewireBusConfig;

import java.util.Set;

/**
 * Raw Home Climate Control configuration, as written in {@code application.yaml}.
 *
 * This configuration needs to be parsed, validated, and materialized to be usable.
 *
 * @param instance HCC instance, to distinguish in metrics.
 * @param esphome ESPHome based devices.
 * @param zigbee2mqtt Zigbee devices, accessed via {@code zigbee2mqtt}
 * @param zwave2mqtt Z-Wave devices, accessed via {@code zwave2mqtt}.
 * @param onewire 1-Wire devices.
 * @param mocks Mock devices, to emulate missing features while in development.
 * @param filters Signal filters.
 * @param zones Zone configurations.
 * @param connectors Incoming and outgoing connectors.
 * @param hvac HVAC hardware devices.
 * @param units HVAC unit abstractions.
 * @param directors Entities tying configuration details together.
 * @param webUi WebUI representation of the whole system.
 * @param console Console representation of the whole system.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@JsonRootName("home-climate-control")
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record HccRawConfig(
        String instance,
        Set<MqttDeviceConfig> esphome,
        Set<MqttDeviceConfig> zigbee2mqtt,
        Set<MqttDeviceConfig> zwave2mqtt,
        Set<OnewireBusConfig> onewire,
        Set<MockConfig> mocks,
        Set<FilterConfig> filters,
        Set<ZoneConfig> zones,
        Set<ConnectorConfig> connectors,
        Set<HvacDeviceConfig> hvac,
        Set<UnitControllerConfig> units,
        Set<UnitDirectorConfig> directors,
        WebUiConfig webUi,
        ConsoleConfig console) {
}
