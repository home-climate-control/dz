package net.sf.dz3.runtime.quarkus;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import net.sf.dz3.runtime.ApplicationBase;
import net.sf.dz3.runtime.config.HccRawConfig;
import net.sf.dz3.runtime.config.quarkus.HccRawInterfaceConfig;
import net.sf.dz3.runtime.mapper.InterfaceRecordMapper;
import org.apache.logging.log4j.ThreadContext;

/**
 * Quarkus entry point into HCC Core.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2023
 */
@ApplicationScoped
public class HccApplication extends ApplicationBase<HccRawInterfaceConfig> {

    /**
     * Injected configuration.
     */
    @Context
    HccRawInterfaceConfig config;

    void onStart(@Observes StartupEvent e) {
        ThreadContext.push("onStart");

        try {
            init();

            // Quarkus quirk; its configuration is quite different from others
            printConfigurationFromInterface();

            run(config);

        } catch (Exception ex) {

            logger.fatal("Unexpected exception: ", ex);
            Thread.currentThread().interrupt();

        } finally {
            logger.fatal("done");
            ThreadContext.pop();
        }
    }

    /**
     * Print the configuration rooted in {@link HccRawInterfaceConfig}.
     */
    private void printConfigurationFromInterface() {

        logger.debug("configuration/interface: {}", () -> {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Failed to convert materialized interface configuration to JSON", ex);
            }
        });
    }

    @Override
    protected HccRawConfig mapConfiguration(HccRawInterfaceConfig config) {
        return InterfaceRecordMapper.INSTANCE.rawConfig(config);
    }

    @GET
    @Path("/hello")
    @Produces("text/plain")
    public String hello() {
        return "Oh, hai";
    }
}
