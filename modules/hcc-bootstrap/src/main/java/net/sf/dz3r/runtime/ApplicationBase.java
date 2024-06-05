package net.sf.dz3r.runtime;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationParser;
import net.sf.dz3r.runtime.config.HccRawConfig;
import net.sf.dz3r.runtime.config.ShutdownHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * HCC core common logic.
 *
 * @param <C> Framework configuration type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public abstract class ApplicationBase<C> {
    protected final Logger logger = LogManager.getLogger();
    protected final ObjectMapper objectMapper;

    protected ApplicationBase() {

        objectMapper = new ObjectMapper(new YAMLFactory());

        // Necessary to print Optionals in a sane way
        objectMapper.registerModule(new Jdk8Module());

        // Necessary to deal with Duration
        objectMapper.registerModule(new JavaTimeModule());

        // For Quarkus to deal with interfaces nicer
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        // For standalone to allow to ignore the root element
        objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    }

    protected final void init() throws IOException {
        ReactorDebugAgent.init();

        // WARN level so that it shows up in a shorter log and is faster to find on a slow box
        logger.warn("Starting up");

        reportGitProperties();

        logger.debug("CPU count reported: {}", Runtime.getRuntime().availableProcessors());
        logger.debug("reactor-core default pool size: {}", Schedulers.DEFAULT_POOL_SIZE);
        logger.debug("reactor-core default bounded elastic size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE);
        logger.debug("reactor-core default bounded elastic queue size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE);
    }

    private void reportGitProperties() {

        // Once in a blue moon, this code fails to read git.properties. Likely a race condition, not an excuse to start up.
        // Besides, it's not on a critical path so we can do it in the background.

        new Thread(() -> {

            ThreadContext.push("git.properties");

            try {
                var p = GitProperties.get();

                logger.debug("git.branch={}", p.get("git.branch"));
                logger.debug("git.commit.id={}", p.get("git.commit.id"));
                logger.debug("git.commit.id.abbrev={}", p.get("git.commit.id.abbrev"));
                logger.debug("git.commit.id.describe={}", p.get("git.commit.id.describe"));
                logger.debug("git.build.version={}", p.get("git.build.version"));

            } catch (IOException ex) {
                logger.error("Failed to read Git properties", ex);
            } finally {
                ThreadContext.pop();
            }

        }).start();
    }

    /**
     * Map framework dependent configuration to {@link HccRawConfig}.
     *
     * @param source Framework dependent configuration.
     * @return Raw framework independent configuration.
     */
    protected abstract HccRawConfig mapConfiguration(C source);

    protected final void run(C rawConfig) throws IOException {

        Marker m = new Marker("run", Level.INFO);
        try {

            var config = mapConfiguration(rawConfig);
            var configYaml = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
            var digest = getDigest(configYaml);

            logger.debug("configuration: digest={}, YAML:\n{}", digest, configYaml);

            m.checkpoint("read configuration");
            var context = new ConfigurationParser().parse(config, digest);
            m.checkpoint("started");

            sleepUntilKilled(context);

        } finally {
            logger.fatal("Shut down");
            m.close();
        }
    }

    private String getDigest(String source) {
        var algorithm = "SHA256";
        try {

            var md = MessageDigest.getInstance(algorithm);
            md.update(source.getBytes(UTF_8));
            var digest = md.digest();

            return HexFormat.of().formatHex(digest);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Can't get " + algorithm + "? Something is seriously wrong", ex);
        }
    }

    private void sleepUntilKilled(ConfigurationContext context) {

        var stopGate = new CountDownLatch(1);

        // ShutdownHandler *is* used - it is AutoCloseable
        try (var ignored = new ShutdownHandler(context)) {

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ThreadContext.push("shutdownHook");
                try {

                    logger.warn("Received termination signal");
                    stopGate.countDown();

                } finally {
                    ThreadContext.pop();
                }
            }));

            // Logged at WARN so that it is easier to see in the log
            logger.warn("Startup complete, sleeping until killed");

            stopGate.await();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted, can do nothing about it", ex);
        } catch (Exception ex) {
            logger.error("Unexpected exception, can do nothing about it", ex);
        }
    }
}
