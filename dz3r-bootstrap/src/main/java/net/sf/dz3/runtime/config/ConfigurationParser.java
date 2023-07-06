package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.connector.ConnectorConfigurationParser;
import net.sf.dz3.runtime.config.filter.FilterConfigurationParser;
import net.sf.dz3.runtime.config.hardware.HvacConfigurationParser;
import net.sf.dz3.runtime.config.hardware.UnitConfigurationParser;
import net.sf.dz3.runtime.config.model.DirectorConfigurationParser;
import net.sf.dz3.runtime.config.model.ZoneConfigurationParser;
import net.sf.dz3.runtime.config.mqtt.MqttConfigurationParser;
import net.sf.dz3.runtime.config.onewire.OnewireConfigurationParser;
import net.sf.dz3r.instrumentation.Marker;
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

        Marker m = new Marker(getClass().getSimpleName() + "#parse");
        try {
            new MqttConfigurationParser().parse(
                    source.esphome(),
                    source.zigbee2mqtt(),
                    source.zwave2mqtt());

            new OnewireConfigurationParser().parse(source.onewire());

            new MockConfigurationParser().parse(source.mocks());

            new FilterConfigurationParser().parse(source.filters());

            new ZoneConfigurationParser().parse(source.zones());

            new ConnectorConfigurationParser().parse(source.connectors());

            new HvacConfigurationParser().parse(source.hvac());

            new UnitConfigurationParser().parse(source.units());

            new DirectorConfigurationParser().parse(source.directors());

            logger.error("ConfigurationParser::parse(): NOT IMPLEMENTED");

            return new HccParsedConfig();
        } finally {
            m.close();
        }
    }

}
