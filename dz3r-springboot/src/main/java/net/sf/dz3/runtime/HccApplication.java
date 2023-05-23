package net.sf.dz3.runtime;

import net.sf.dz3.runtime.config.HccConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(HccConfiguration.class)
public class HccApplication implements CommandLineRunner {

    private final HccConfiguration config;

    private static final Logger logger = LogManager.getLogger(HccApplication.class);
    public HccApplication(HccConfiguration config) {
        this.config = config;
    }

    public static void main(String[] args) {

        SpringApplication.run(HccApplication.class, args);
    }

    @Override
    public void run(String... args) {
        logger.info("configuration: {}", config);
        logger.info("");
        logger.fatal("DON'T YOU EVER HOPE THIS WORKS. MORE WORK UNDERWAY, STAY TUNED");
    }
}