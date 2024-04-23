package net.sf.dz3r.runtime.config.quarkus;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.smallrye.config.ConfigMapping;
import net.sf.dz3r.runtime.config.HccRawConfig;
import net.sf.dz3r.runtime.config.quarkus.connector.ConnectorConfig;
import net.sf.dz3r.runtime.config.quarkus.filter.FilterConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.HvacDeviceConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.MockConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.UnitControllerConfig;
import net.sf.dz3r.runtime.config.quarkus.model.ConsoleConfig;
import net.sf.dz3r.runtime.config.quarkus.model.MeasurementUnits;
import net.sf.dz3r.runtime.config.quarkus.model.UnitDirectorConfig;
import net.sf.dz3r.runtime.config.quarkus.model.WebUiConfig;
import net.sf.dz3r.runtime.config.quarkus.model.ZoneConfig;
import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.runtime.config.quarkus.protocol.onewire.OnewireBusConfig;
import net.sf.dz3r.runtime.config.quarkus.schedule.ScheduleConfig;

import java.util.Optional;
import java.util.Set;

/**
 * Raw Home Climate Control configuration, as written in {@code application.yaml}.
 *
 * This configuration mirrors {@link HccRawConfig}, and {@code HccRawRecordConfig}, only in a Quarkus compatible way.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@ConfigMapping(prefix = "home-climate-control")
public interface HccRawInterfaceConfig {
    @JsonProperty("instance")
    String instance();
    @JsonProperty("measurement-units")
    Optional<MeasurementUnits> measurementUnits();
    @JsonProperty("esphome")
    Set<MqttDeviceConfig> esphome();
    @JsonProperty("zigbee2mqtt")
    Set<MqttDeviceConfig> zigbee2mqtt();
    @JsonProperty("zwave2mqtt")
    Set<MqttDeviceConfig> zwave2mqtt();
    @JsonProperty("onewire")
    Set<OnewireBusConfig> onewire();
    @JsonProperty("mocks")
    Set<MockConfig> mocks();
    @JsonProperty("filters")
    Set<FilterConfig> filters();
    @JsonProperty("hvac")
    Set<HvacDeviceConfig> hvac();
    @JsonProperty("zones")
    Set<ZoneConfig> zones();
    @JsonProperty("schedule")
    ScheduleConfig schedule();
    @JsonProperty("connectors")
    Set<ConnectorConfig> connectors();
    @JsonProperty("units")
    Set<UnitControllerConfig> units();
    @JsonProperty("directors")
    Set<UnitDirectorConfig> directors();
    @JsonProperty("web-ui")
    Optional<WebUiConfig> webUi();
    @JsonProperty("console")
    Optional<ConsoleConfig> console();
}
