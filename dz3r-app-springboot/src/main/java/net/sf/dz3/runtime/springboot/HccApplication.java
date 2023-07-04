package net.sf.dz3.runtime.springboot;

import net.sf.dz3.runtime.ApplicationBase;
import net.sf.dz3.runtime.config.HccRawConfig;
import net.sf.dz3.runtime.config.HccRawRecordConfig;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * SpringBoot entry point into HCC Core.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2023
 */
@SpringBootApplication
@EnableConfigurationProperties(HccRawRecordConfig.class)
public class HccApplication extends ApplicationBase<HccRawRecordConfig> implements CommandLineRunner {

    /**
     * Injected configuration.
     */
    private final HccRawRecordConfig config;

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
            init();

            // VT: NOTE: Do we really use any here?
            logger.info("command line arguments: {}", (Object[]) args);

            run(config);

        } catch (Exception ex) {

            logger.fatal("Unexpected exception: ", ex);
            Thread.currentThread().interrupt();

        } finally {
            logger.fatal("Shutting down");
            ThreadContext.pop();
        }
    }

    /**
     * Map the Spring annotation tainted configuration into framework independent.
     *
     * @param source Configuration with Spring annotation on it.
     * @return Configuration without any annotations.
     */
    @Override
    protected HccRawConfig mapConfiguration(HccRawRecordConfig source) {
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
