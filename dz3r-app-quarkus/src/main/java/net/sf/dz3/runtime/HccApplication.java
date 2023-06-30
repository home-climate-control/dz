package net.sf.dz3.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import net.sf.dz3.runtime.config.ConfigurationParser;
import net.sf.dz3.runtime.config.HccRawConfig;
import net.sf.dz3.runtime.config.quarkus.HccRawInterfaceConfig;
import net.sf.dz3.runtime.mapper.InterfaceRecordMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import reactor.tools.agent.ReactorDebugAgent;

@ApplicationScoped
public class HccApplication {
    private final Logger logger = LogManager.getLogger();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ConfigProperty(name = "hcc.debug.config.from-interface", defaultValue = "false")
    boolean printConfigFromInterface;

    @ConfigProperty(name = "hcc.debug.config.from-record", defaultValue = "true")
    boolean printConfigFromRecord;

    @Context
    HccRawInterfaceConfig config;

    void onStart(@Observes StartupEvent e) {

        ThreadContext.push("onStart");
        try {
            ReactorDebugAgent.init();
            logger.warn("Starting up");

            printConfigurationFromInterface();
            new ConfigurationParser().parse(map(config)).start().block();

            logger.warn("run complete");

            logger.info("");
            logger.fatal("DON'T YOU EVER HOPE THIS WORKS. MORE WORK UNDERWAY, STAY TUNED");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Print the configuration rooted in {@link HccRawInterfaceConfig}.
     */
    private void printConfigurationFromInterface() {

        if (!printConfigFromInterface) {
            logger.info("configured not to print config instantiated from interface");
            return;
        }

        // Necessary to print Optionals in a sane way
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        logger.info("configuration/interface: {}", () -> {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Failed to convert materialized interface configuration to JSON", ex);
            }
        });
    }

    private HccRawConfig map(HccRawInterfaceConfig config) {

        var recordConfig = InterfaceRecordMapper.INSTANCE.rawConfig(config);

        if (printConfigFromRecord) {
            logger.info("configurations/record: {}", () -> {
                try {
                    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(recordConfig);
                } catch (JsonProcessingException ex) {
                    throw new IllegalStateException("Failed to convert materialized record configuration to JSON", ex);
                }
            });
        } else {
            logger.info("configured not to print config instantiated from record");
        }

        return recordConfig;
    }

    @GET
    @Path("/hello")
    @Produces("text/plain")
    public String hello() {
        return "Oh, hai";
    }
}
