package net.sf.dz3.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.dz3.runtime.config.ConfigurationParser;
import net.sf.dz3.runtime.config.HccRawConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
@EnableConfigurationProperties(HccRawConfig.class)
public class HccApplication implements CommandLineRunner {

    private final HccRawConfig config;

    private static final Logger logger = LogManager.getLogger(HccApplication.class);
    public HccApplication(HccRawConfig config) {
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
            logger.info("configuration: {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(config));

            new ConfigurationParser().parse(config).start().block();

            logger.info("run complete");

            logger.info("");
            logger.fatal("DON'T YOU EVER HOPE THIS WORKS. MORE WORK UNDERWAY, STAY TUNED");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
            ThreadContext.pop();
        }
    }
}
