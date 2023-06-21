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
import net.sf.dz3.runtime.config.quarkus.HccRawConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

@ApplicationScoped
public class HccApplication {
    private final Logger logger = LogManager.getLogger();

    @Context
    HccRawConfig config;

    void onStart(@Observes StartupEvent e) {

        ThreadContext.push("onStart");
        try {
            logger.warn("Starting up");

            printConfiguration();
            // VT: FIXME: Nothing to do now, need to materialize the app first, stay tuned

        } finally {
            ThreadContext.pop();
        }
    }

    private void printConfiguration() {
        try {
            var mapper = new ObjectMapper();

            // Necessary to print Optionals in a sane way
            mapper.registerModule(new Jdk8Module());
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

            logger.info("configuration: {}",
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));

        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to convert materialized YAML configuration to JSON", ex);
        }
    }

    @GET
    @Path("/hello")
    @Produces("text/plain")
    public String hello() {
        return "Oh, hai";
    }
}
