package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.connector.ConnectorConfigurationParser;
import net.sf.dz3.runtime.config.filter.FilterConfigurationParser;
import net.sf.dz3.runtime.config.hardware.HvacConfigurationParser;
import net.sf.dz3.runtime.config.hardware.UnitConfigurationParser;
import net.sf.dz3.runtime.config.model.ConsoleConfigurationParser;
import net.sf.dz3.runtime.config.model.DirectorConfigurationParser;
import net.sf.dz3.runtime.config.model.WebUiConfigurationParser;
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

            var ctx = new ConfigurationContext();

            new MqttConfigurationParser(ctx).parse(
                    source.esphome(),
                    source.zigbee2mqtt(),
                    source.zwave2mqtt());

            new OnewireConfigurationParser(ctx).parse(source.onewire());

            new MockConfigurationParser(ctx).parse(source.mocks());

            new FilterConfigurationParser(ctx).parse(source.filters());

            // There will be no more sensors and switches coming after this
            ctx.sensors.close();
            ctx.switches.close();

            new ZoneConfigurationParser(ctx).parse(source.zones());
            ctx.zones.close();

            new ConnectorConfigurationParser(ctx).parse(source.connectors());
            ctx.collectors.close();
            ctx.connectors.close();

            new HvacConfigurationParser(ctx).parse(source.hvac());
            ctx.hvacDevices.close();

            new UnitConfigurationParser(ctx).parse(source.units());
            ctx.units.close();

            new DirectorConfigurationParser(ctx).parse(source.directors());
            ctx.directors.close();

            new WebUiConfigurationParser(ctx).parse(source.webUi());

            new ConsoleConfigurationParser(ctx).parse(source.console());

            logger.error("ConfigurationParser::parse(): NOT IMPLEMENTED");

            return new HccParsedConfig();
        } finally {
            m.close();
        }
    }
}
