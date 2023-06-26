package net.sf.dz3.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.dz3.runtime.config.ConfigurationParser;
import net.sf.dz3.runtime.config.HccRawConfig;
import net.sf.dz3.runtime.config.HccRawRecordConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
@EnableConfigurationProperties(HccRawRecordConfig.class)
public class HccApplication implements CommandLineRunner {

    private final HccRawRecordConfig config;

    private static final Logger logger = LogManager.getLogger(HccApplication.class);
    public HccApplication(HccRawRecordConfig config) {
        this.config = config;
    }

    public static void main(String[] args) {

        // VT: NOTE: call close() or exit() on this when all the kinks are ironed out, to support a controlled lifecycle
        SpringApplication.run(HccApplication.class, args);
    }

    @Override
    public void run(String... args) {
        ThreadContext.push("run");

        try {
            ReactorDebugAgent.init();

            logger.info("command line arguments: {}", (Object[]) args);
            logger.info("configuration: {}", () -> {
                try {
                    return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(config);
                } catch (JsonProcessingException ex) {
                    throw new IllegalStateException("Failed to convert materialized record configuration to JSON", ex);
                }
            });

            new ConfigurationParser().parse(map(config)).start().block();

            logger.warn("run complete");

            logger.info("");
            logger.fatal("DON'T YOU EVER HOPE THIS WORKS. MORE WORK UNDERWAY, STAY TUNED");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Map the Spring annotation tainted configuration into framework independent.
     *
     * @param source Configuration with Spring annotation on it.
     * @return Configuration without any annotations.
     */
    private HccRawConfig map(HccRawRecordConfig source) {
        return new HccRawConfig(
                source.instance(),
                source.esphome(),
                source.zigbee2mqtt(),
                source.zwave2mqtt(),
                source.onewire(),
                source.mocks(),
                source.filters(),
                source.zones(),
                source.connectors(),
                source.hvac(),
                source.units(),
                source.directors(),
                source.webUi(),
                source.console()
        );
    }
}
