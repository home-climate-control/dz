package net.sf.dz3.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.sf.dz3.runtime.config.ConfigurationParser;
import net.sf.dz3.runtime.config.HccRawConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

/**
 * HCC core common logic.
 *
 * @param <C> Framework configuration type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2023
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

    protected final void init() {
        ReactorDebugAgent.init();

        // WARN level so that it shows up in a shorter log and is faster to find on a slow box
        logger.warn("Starting up");

        logger.debug("CPU count reported: {}", Runtime.getRuntime().availableProcessors());
        logger.debug("reactor-core default pool size: {}", Schedulers.DEFAULT_POOL_SIZE);
        logger.debug("reactor-core default bounded elastic size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE);
        logger.debug("reactor-core default bounded elastic queue size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE);
    }

    /**
     * Map framework dependent configuration to {@link HccRawConfig}.
     *
     * @param source Framework dependent configuration.
     * @return Raw framework independent configuration.
     */
    protected abstract HccRawConfig mapConfiguration(C source);

    protected final void run(C rawConfig) {

        var config = mapConfiguration(rawConfig);

        logger.debug("configuration: {}", () -> {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Failed to convert materialized record configuration to JSON", ex);
            }
        });

        new ConfigurationParser().parse(config).start().block();

        logger.warn("run complete");

        logger.info("");
        logger.fatal("DON'T YOU EVER HOPE THIS WORKS. MORE WORK UNDERWAY, STAY TUNED");
    }
}