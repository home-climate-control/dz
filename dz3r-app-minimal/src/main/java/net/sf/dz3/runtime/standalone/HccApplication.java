package net.sf.dz3.runtime.standalone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.sf.dz3.runtime.config.ConfigurationParser;
import net.sf.dz3.runtime.config.HccRawConfig;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Entry point into HCC Core.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2023
 */
public class HccApplication {

    /**
     * Logger to use.
     */
    private final Logger logger = LogManager.getLogger();

    /**
     * Run the application.
     * @param args Configuration location.
     */
    public static void main(String[] args) {
        new HccApplication().run(args);
    }

    /**
     * Run the system.
     */
    @SuppressWarnings({"squid::S2189", "squid:S1181"})
    public void run(String[] args) {

        ThreadContext.push("run");

        try {
            ReactorDebugAgent.init();

            // WARN level so that it shows up in a shorter log and is faster to find on a slow box
            logger.warn("Starting up");

            logger.debug("CPU count reported: {}", Runtime.getRuntime().availableProcessors());
            logger.debug("reactor-core default pool size: {}", Schedulers.DEFAULT_POOL_SIZE);
            logger.debug("reactor-core default bounded elastic size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE);
            logger.debug("reactor-core default bounded elastic queue size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE);

            if (args.length == 0) {

                logger.error("Usage: dz <configuration file>");
                return;
            }

            var config = loadConfiguration(args[0]);

            new ConfigurationParser().parse(config).start().block();

            logger.warn("run complete");

            logger.info("");
            logger.fatal("DON'T YOU EVER HOPE THIS WORKS. MORE WORK UNDERWAY, STAY TUNED");

        } catch (Throwable t) {

            // squid:S1181: No.
            logger.fatal("Unexpected exception: ", t);
            Thread.currentThread().interrupt();

        } finally {
            logger.fatal("Shutting down");
            ThreadContext.pop();
        }
    }

    /**
     * Load the configuration.
     *
     * @param source Configuration source.
     */
    private HccRawConfig loadConfiguration(String source) {

        ThreadContext.push("loadConfiguration(" + source + ")");
        var m = new Marker("loadConfiguration(" + source + ")");

        try {

            try {

                if (source.startsWith("/")) {
                    logger.warn("Absolute location, using Spring file: quirk");
                    source = "file:" + source;
                }

                var objectMapper = new ObjectMapper(new YAMLFactory());
                objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);

                var config = objectMapper.readValue(getStream(source), HccRawConfig.class);

                logger.debug("configuration: {}", () -> {
                    try {
                        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(config);
                    } catch (JsonProcessingException ex) {
                        throw new IllegalStateException("Failed to convert materialized record configuration to JSON", ex);
                    }
                });

                return config;

            } catch (Exception ex) {
                throw new IllegalArgumentException("Unexpected exception while parsing " + source,  ex);
            }

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    /**
     * Get the stream from the given location.
     *
     * @param source Source location.
     * @return The input stream.
     * @throws IOException if things go sour.
     */
    InputStream getStream(String source) throws IOException {

        try {

            return getStreamAsFile(source);

        } catch (IOException ex) {

            logger.trace("not a file: {}", source, ex);

            return getStreamAsURL(source);
        }
    }

    /**
     * Get the stream from the given file location.
     *
     * @param source Source location as a file name.
     * @return Source stream.
     * @throws IOException if the {@code source} is not a file, or other I/O problem occurred.
     */
    private InputStream getStreamAsFile(String source) throws IOException {
        return new FileInputStream(source);
    }

    /**
     * Get the stream from the given URL.
     *
     * @param source Source location as a URL.
     * @return Source stream.
     * @throws IOException if the {@code source} is not a URL, or other I/O problem occurred.
     */
    private InputStream getStreamAsURL(String source) throws IOException {
        return new URL(source).openStream();
    }
}
