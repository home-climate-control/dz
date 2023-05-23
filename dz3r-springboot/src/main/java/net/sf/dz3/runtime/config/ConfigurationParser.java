package net.sf.dz3.runtime.config;

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

        logger.error("ConfigurationParser::parse(): NOT IMPLEMENTED");
        return new HccParsedConfig();
    }
}
