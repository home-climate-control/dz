package net.sf.dz3.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.dz3.runtime.config.HccRawConfig;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;
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
public class Container {

    /**
     * Logger to use.
     */
    private static final Logger logger = LogManager.getLogger(Container.class);

    private final Yaml yaml = new Yaml();

    /**
     * Name of the file this class expects to find the configuration in.
     *
     * Must be on the root of the classpath.
     */
    public static final String CF_PI = "raspberry-pi.xml";

    /**
     * Run the application.
     * @param args Configuration location.
     */
    public static void main(String[] args) {

        new Container().run(args);
    }

    /**
     * Run the system.
     */
    @SuppressWarnings({"squid::S2189", "squid:S1181"})
    public void run(String[] args) {

        ThreadContext.push("run");

        // WARN level so that it shows up in a shorter log and is faster to find on a slow box
        logger.warn("Starting up");

        logger.debug("CPU count reported: {}", Runtime.getRuntime().availableProcessors());
        logger.debug("reactor-core default pool size: {}", Schedulers.DEFAULT_POOL_SIZE);
        logger.debug("reactor-core default bounded elastic size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE);
        logger.debug("reactor-core default bounded elastic queue size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE);

        try {
            ReactorDebugAgent.init();

            var configFound = false;

            if (args.length == 0) {

                logger.warn("Usage: dz <configuration file>");
                logger.warn("Trying to load a sample configuration from '{}'", CF_PI);

                configFound = loadConfiguration(CF_PI);

            } else {
                for (String arg : args) {
                    configFound = loadConfiguration(arg) || configFound;
                }
            }

            if (!configFound) {
                logger.error("No configuration was found, terminating");
                return;
            }

            logger.info("Sleeping until killed");

            // squid:S2189: Works as designed.
            synchronized (this) {
                while (true) {
                    wait(10000);
                    if (Thread.interrupted()) {
                        logger.info("Interrupted, terminating");
                        break;
                    }
                }
            }

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
     *
     * @see #CF_PI
     */
    private boolean loadConfiguration(String source) {

        ThreadContext.push("loadConfiguration(" + source + ")");
        var m = new Marker("loadConfiguration(" + source + ")");

        try {

            // Classpath loading is much less likely, let's try this first
            if (loadFromPath(source)) {
                return true;
            }

            return loadFromClasspath(source);

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    /**
     * Load the configuration from a file.
     *
     * @param source File to load the configuration from.
     * @return {@code true} if an attempt succeeded.
     */
    private boolean loadFromPath(String source) {
        try {

            if (source.startsWith("/")) {
                logger.warn("Absolute location, using Spring file: quirk");
                source = "file:" + source;
            }

            HccRawConfig config = yaml.loadAs(getStream(source), HccRawConfig.class);
            logger.info("configuration: {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(config));

            return true;

        } catch (ScannerException ex) {
            throw new IllegalArgumentException("Malformed YAML while parsing " + source, ex);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unexpected exception while parsing " + source,  ex);
        }
    }

    private boolean loadFromClasspath(String source) {
        throw new UnsupportedOperationException("Not Implemented");
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
