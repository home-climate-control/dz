package net.sf.dz3.runtime.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * Home Climate Control configuration parsed from the {@link HccRawConfig raw}, ready to be started.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class HccParsedConfig {

    private final Logger logger = LogManager.getLogger();

    /**
     * Start the configuration.
     *
     * @return a {@link Mono} that completes when the constellation started stops.
     */
    public Mono<Void> start() {

        logger.error("HccParsedConfig::start(): NOT IMPLEMENTED");
        return Mono.empty();
    }
}
