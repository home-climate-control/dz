package net.sf.dz3r.runtime.standalone;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import net.sf.dz3r.runtime.ApplicationBase;
import net.sf.dz3r.runtime.config.HccRawConfig;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Standalone entry point into HCC Core.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2023
 */
public class HccApplication extends ApplicationBase<HccRawConfig> {

    /**
     * Run the application.
     * @param args Configuration location.
     */
    public static void main(String[] args) {
        new HccApplication().run(args);
    }

    public void run(String[] args) {
        ThreadContext.push("run");

        try {
            init();

            if (args.length == 0) {

                logger.error("Usage: dz <configuration file>");
                return;
            }

            run(loadConfiguration(args[0]));

        } catch (Exception ex) {

            logger.fatal("Unexpected exception: ", ex);
            Thread.currentThread().interrupt();

        } finally {
            logger.fatal("done");
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

                return objectMapper.readValue(getStream(source), HccRawConfig.class);

            } catch (IOException ex) {

                var message = ex.getMessage();

                // VT: NOTE: Both Quarkus and Spring use a trick to bypass this; would be nice to figure out what it is.
                // For now, using the format documented at the link will yield a working configuration.

                if (ex instanceof InvalidFormatException && message != null && message.startsWith("Cannot deserialize value of type `java.time.Duration` from String")) {
                    throw new IllegalArgumentException("Try to use duration format specified in https://en.wikipedia.org/wiki/ISO_8601#Durations", ex);
                }

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

    @Override
    protected HccRawConfig mapConfiguration(HccRawConfig source) {
        // No mapping necessary
        return source;
    }
}
