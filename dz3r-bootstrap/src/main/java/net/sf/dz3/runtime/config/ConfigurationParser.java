package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.mqtt.EspSensorSwitchResolver;
import net.sf.dz3.runtime.config.mqtt.MqttConfgurationParser;
import net.sf.dz3.runtime.config.mqtt.ZWaveSensorSwitchResolver;
import net.sf.dz3.runtime.config.mqtt.ZigbeeSensorSwitchResolver;
import net.sf.dz3.runtime.config.onewire.OnewireConfigurationParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

/**
 * Parses {@link HccRawConfig} into {@link HccParsedConfig}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ConfigurationParser {
    private final Logger logger = LogManager.getLogger();

    public HccParsedConfig parse(HccRawConfig source) {

        new MqttConfgurationParser().parse(Flux
                .just(
                        new EspSensorSwitchResolver(source.esphome()),
                        new ZigbeeSensorSwitchResolver(source.zigbee2mqtt()),
                        new ZWaveSensorSwitchResolver(source.zwave2mqtt())
                ));

        new OnewireConfigurationParser().parse(Flux
                .just(source.onewire()));

        logger.error("ConfigurationParser::parse(): NOT IMPLEMENTED");
        return new HccParsedConfig();
    }

}
