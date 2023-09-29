package net.sf.dz3r.view.ha;

import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.view.Connector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class HomeAssistantConnector implements Connector {

    private final Logger logger = LogManager.getLogger();

    private final Config config;
    private final MqttAdapter mqttAdapter;
    private final Set<Zone> zonesConfigured;
    public HomeAssistantConnector(String id, MqttAdapter mqttAdapter, String discoveryPrefix, Set<Zone> zonesConfigured) {
        this.config = new Config(id, discoveryPrefix);
        this.mqttAdapter = mqttAdapter;
        this.zonesConfigured = Collections.unmodifiableSet(zonesConfigured);
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void connect(String unitId, UnitDirector.Feed feed) {

        ThreadContext.push("connect");
        try {

            var zones = getExposedZones(feed.sensorFlux2zone.values());

            Flux
                    .fromIterable(zones)
                    // No need to wait for this
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(this::exposeZone);

            // ...

        } finally {
            ThreadContext.pop();
        }
    }

    private void exposeZone(Zone zone) {
        ThreadContext.push("exposeZone: " + zone.getAddress());

        try {

            var errorCounter = checkCharacters("config.id", config.id) ? 0 : 1;
            errorCounter += checkCharacters("zones.name", zone.getAddress()) ? 0 : 1;

            if (errorCounter > 0) {
                logger.error("zone skipped: {}", zone.getAddress());
                return;
            }

            var configTopic =
                    config.discoveryPrefix
                    + "/climate/"
                    + config.id
                    + "/" + zone.getAddress()
                    + "/config";

            logger.debug("config topic: {}", configTopic);

        } finally {
            ThreadContext.pop();
        }
    }

    private final Pattern pattern = Pattern.compile("[^A-Za-z0-9_-]");

    private boolean checkCharacters(String target, String value) {

        if (pattern.matcher(value).find()) {
            logger.error("{} contains characters outside of allowed [A-Za-z0-9_-] range: {}", target, value);
            return false;
        }

        return true;
    }

    /**
     * Get the set of zones that will be actually exposed.
     *
     * @param feedZones Zones provided by the {@link net.sf.dz3r.model.UnitDirector.Feed}.
     *
     * @return Set of zones that will actually be exposed.
     */
    private Set<Zone> getExposedZones(Collection<Zone> feedZones) {

        // Need to make a copy, retainAll() will modify this
        var result = new LinkedHashSet<>(feedZones);

        logger.debug("connected zones ({} total):", result.size());
        for (var z : result) {
            logger.debug("  {}", z.getAddress());
        }

        result.retainAll(zonesConfigured);

        logger.info("exposed zones ({} total):", result.size());
        for (var z : result) {
            logger.info("  {}", z.getAddress());
        }

        if (result.size() < zonesConfigured.size()) {

            var leftovers = new TreeSet<>(zonesConfigured);
            leftovers.removeAll(result);

            logger.warn("not all configured zones were exposed, here's what's left ({} total):", leftovers.size());
            for (var z : leftovers) {
                logger.warn("  {}", z.getAddress());
            }
        }

        return result;
    }

    private record Config(
            String id,
            String discoveryPrefix
    ) {

    }
}
