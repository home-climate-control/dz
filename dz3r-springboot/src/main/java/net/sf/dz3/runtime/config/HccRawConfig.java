package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.connector.ConnectorConfig;
import net.sf.dz3.runtime.config.hardware.ESPHomeListenerConfig;
import net.sf.dz3.runtime.config.hardware.HvacDeviceConfig;
import net.sf.dz3.runtime.config.hardware.MockConfig;
import net.sf.dz3.runtime.config.hardware.OnewireBusConfig;
import net.sf.dz3.runtime.config.hardware.UnitControllerConfig;
import net.sf.dz3.runtime.config.hardware.Z2MJsonListenerConfig;
import net.sf.dz3.runtime.config.hardware.ZWaveListenerConfig;
import net.sf.dz3.runtime.config.model.ConsoleConfig;
import net.sf.dz3.runtime.config.model.UnitDirectorConfig;
import net.sf.dz3.runtime.config.model.WebUiConfig;
import net.sf.dz3.runtime.config.model.ZoneConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Raw Home Climate Control configuration, as written in {@code application.yaml}.
 *
 * This configuration needs to be parsed, validated, and materialized to be usable.
 *
 * @param instance HCC instance, to distinguish in metrics.
 * @param esphome ESPHome based devices.
 * @param zigbee2mqtt Zigbee devices, accessed via {@code zigbee2mqtt}
 * @param zwave Z-Wave devices, accessed via {@code zwave2mqtt}.
 * @param onewire 1-Wire devices.
 * @param mock Mock devices, to emulate missing features while in development.
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
@ConfigurationProperties(prefix = "home-climate-control")
public record HccRawConfig(
        String instance,
        List<ESPHomeListenerConfig> esphome,
        List<Z2MJsonListenerConfig> zigbee2mqtt,
        List<ZWaveListenerConfig> zwave,
        List<OnewireBusConfig> onewire,
        List<MockConfig> mock,
        List<FilterConfig> filters,
        List<ZoneConfig> zones,
        List<ConnectorConfig> connectors,
        List<HvacDeviceConfig> hvac,
        List<UnitControllerConfig> units,
        List<UnitDirectorConfig> directors,
        WebUiConfig webUi,
        ConsoleConfig console) {
}
