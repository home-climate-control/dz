package net.sf.dz3r.runtime.config;

import net.sf.dz3r.runtime.config.connector.ConnectorConfig;
import net.sf.dz3r.runtime.config.filter.FilterConfig;
import net.sf.dz3r.runtime.config.hardware.HvacDeviceConfig;
import net.sf.dz3r.runtime.config.hardware.MockConfig;
import net.sf.dz3r.runtime.config.hardware.UnitControllerConfig;
import net.sf.dz3r.runtime.config.model.ConsoleConfig;
import net.sf.dz3r.runtime.config.model.MeasurementUnits;
import net.sf.dz3r.runtime.config.model.UnitDirectorConfig;
import net.sf.dz3r.runtime.config.model.WebUiConfig;
import net.sf.dz3r.runtime.config.model.ZoneConfig;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.runtime.config.protocol.onewire.OnewireBusConfig;
import net.sf.dz3r.runtime.config.schedule.ScheduleConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Raw Home Climate Control configuration, as written in {@code application.yaml}.
 *
 * This configuration mirrors {@link HccRawConfig}, only with {@code @ConfigurationProperties} annotation, purpose being
 * to avoid poisoning the core logic with Spring dependencies. The downside is code duplication, records are not
 * polymorphic.
 *
 * @param instance HCC instance, to distinguish in metrics.
 * @param measurementUnits System wide measurement units to use in configuration and display.
 * @param esphome ESPHome based devices.
 * @param zigbee2mqtt Zigbee devices, accessed via {@code zigbee2mqtt}
 * @param zwave2mqtt Z-Wave devices, accessed via {@code zwave2mqtt}.
 * @param onewire 1-Wire devices.
 * @param mocks Mock devices, to emulate missing features while in development.
 * @param filters Signal filters.
 * @param hvac HVAC hardware devices.
 * @param zones Zone configurations.
 * @param schedule Schedule configuration.
 * @param connectors Incoming and outgoing connectors.
 * @param units HVAC unit abstractions.
 * @param directors Entities tying configuration details together.
 * @param webUi WebUI representation of the whole system.
 * @param console Console representation of the whole system.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@ConfigurationProperties(prefix = "home-climate-control")
public record HccRawRecordConfig(
        String instance,
        MeasurementUnits measurementUnits,
        Set<MqttDeviceConfig> esphome,
        Set<MqttDeviceConfig> zigbee2mqtt,
        Set<MqttDeviceConfig> zwave2mqtt,
        Set<OnewireBusConfig> onewire,
        Set<MockConfig> mocks,
        Set<FilterConfig> filters,
        Set<HvacDeviceConfig> hvac,
        Set<ZoneConfig> zones,
        ScheduleConfig schedule,
        Set<ConnectorConfig> connectors,
        Set<UnitControllerConfig> units,
        Set<UnitDirectorConfig> directors,
        WebUiConfig webUi,
        ConsoleConfig console) {
}
