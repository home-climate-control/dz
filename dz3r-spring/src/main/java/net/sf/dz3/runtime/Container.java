package net.sf.dz3.runtime;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Duration;

/**
 * Entry point into DZ Core.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2023
 */
public class Container {

    /**
     * Logger to use.
     */
    private static final Logger logger = LogManager.getLogger(Container.class);

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

            // Start early so that we have a hint of what is going on
            startMicrometerRegistry();

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

    private void startMicrometerRegistry() {

        ThreadContext.push("startMicrometerRegistry");

        try {

            // Different from whatever is used by InfluxDbLogger
            final var dbName = getSystemProperty("HCC_MICROMETER_INFLUXDB_DB", "database name", "hcc-micrometer");
            final var uri = getSystemProperty("HCC_MICROMETER_INFLUXDB_URI", "uri", "http://localhost:8086");

            InfluxConfig config = new InfluxConfig() {
                @Override
                public Duration step() {
                    return Duration.ofSeconds(10);
                }

                @Override
                public String db() {
                    return dbName;
                }

                @Override
                public String uri() {
                    return uri;
                }

                @Override
                public String get(String key) {
                    return null; // accept the rest of the defaults
                }
            };

            MeterRegistry registry = new InfluxMeterRegistry(config, Clock.SYSTEM);
            Metrics.addRegistry(registry);

            new JvmGcMetrics().bindTo(registry);
            new JvmHeapPressureMetrics().bindTo(registry);
            new JvmMemoryMetrics().bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
            new Log4j2Metrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
            new UptimeMetrics().bindTo(registry);

        } finally {
            ThreadContext.pop();
        }
    }

    private String getSystemProperty(String key, String description, String defaultValue) {

        var value = System.getProperty(key);

        if (value == null) {
            logger.warn("Default Micrometer InfluxDB {} ({}) is used, override with system property {}=<value>", description, defaultValue, key);
            return defaultValue;
        }

        logger.info("Micrometer InfluxDB {description}={}", value);

        return value;
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

            AbstractApplicationContext applicationContext = new FileSystemXmlApplicationContext(source);

            applicationContext.registerShutdownHook();
            return true;

        } catch (BeanDefinitionStoreException ex) {

            logger.warn("Failed to load {}, reason: {}", source, ex.getMessage());
            return false;
        }
    }

    private boolean loadFromClasspath(String source) {
        try {

            AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext(source);

            applicationContext.registerShutdownHook();
            return true;

        } catch (BeanDefinitionStoreException ex) {

            logger.warn("Failed to load {}, reason: {}", source, ex.getMessage());
            return false;
        }
    }
}
