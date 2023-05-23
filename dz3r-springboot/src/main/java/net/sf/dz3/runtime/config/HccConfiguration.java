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

@ConfigurationProperties(prefix = "home-climate-control")
public record HccConfiguration(
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
