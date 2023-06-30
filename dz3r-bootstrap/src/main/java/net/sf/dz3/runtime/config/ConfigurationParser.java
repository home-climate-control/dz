package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.mqtt.MqttConfgurationParser;
import net.sf.dz3.runtime.config.onewire.OnewireConfigurationParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses {@link HccRawConfig} into {@link HccParsedConfig}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ConfigurationParser {
    private final Logger logger = LogManager.getLogger();

    public HccParsedConfig parse(HccRawConfig source) {

        new MqttConfgurationParser().parse(
                source.esphome(),
                source.zigbee2mqtt(),
                source.zwave2mqtt());

        new OnewireConfigurationParser().parse(source.onewire());

        logger.error("ConfigurationParser::parse(): NOT IMPLEMENTED");
        return new HccParsedConfig();
    }

}
