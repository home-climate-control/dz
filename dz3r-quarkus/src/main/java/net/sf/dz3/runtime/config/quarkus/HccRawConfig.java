package net.sf.dz3.runtime.config.quarkus;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.smallrye.config.ConfigMapping;
import net.sf.dz3.runtime.config.quarkus.connector.ConnectorConfig;
import net.sf.dz3.runtime.config.quarkus.hardware.HvacDeviceConfig;
import net.sf.dz3.runtime.config.quarkus.hardware.MockConfig;
import net.sf.dz3.runtime.config.quarkus.hardware.UnitControllerConfig;
import net.sf.dz3.runtime.config.quarkus.model.ConsoleConfig;
import net.sf.dz3.runtime.config.quarkus.model.UnitDirectorConfig;
import net.sf.dz3.runtime.config.quarkus.model.WebUiConfig;
import net.sf.dz3.runtime.config.quarkus.model.ZoneConfig;
import net.sf.dz3.runtime.config.quarkus.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.quarkus.protocol.onewire.OnewireBusConfig;

import java.util.Set;

/**
 * Raw Home Climate Control configuration, as written in {@code application.yaml}.
 *
 * This configuration needs to be parsed, validated, and materialized to be usable.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@ConfigMapping(prefix = "home-climate-control")
public interface HccRawConfig {
    @JsonProperty("instance")
    String instance();
    @JsonProperty("esphome")
    Set<MqttDeviceConfig> esphome();
    @JsonProperty("zigbee2mqtt")
    Set<MqttDeviceConfig> zigbee2mqtt();
    @JsonProperty("zwave2mqtt")
    Set<MqttDeviceConfig> zwave2mqtt();
    @JsonProperty("onewire")
    Set<OnewireBusConfig> onewire();
    @JsonProperty("mock")
    Set<MockConfig> mock();
    @JsonProperty("filters")
    Set<FilterConfig> filters();
    @JsonProperty("zones")
    Set<ZoneConfig> zones();
    @JsonProperty("connectors")
    Set<ConnectorConfig> connectors();
    @JsonProperty("hvac")
    Set<HvacDeviceConfig> hvac();
    @JsonProperty("units")
    Set<UnitControllerConfig> units();
    @JsonProperty("directors")
    Set<UnitDirectorConfig> directors();
    @JsonProperty("web-ui")
    WebUiConfig webUi();
    @JsonProperty("console")
    ConsoleConfig console();
}
