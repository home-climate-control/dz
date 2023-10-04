package net.sf.dz3r.view.ha;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.device.mqtt.v1.MqttSignal;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.Connector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;


/**
 * <a href="https://homeassistant.io">Home Assistant</a> integration.
 *
 * References:
 *
 * <ul>
 * <li>
 *     https://github.com/home-climate-control/dz/blob/gh286-ha/docs/configuration/home-assistant.md - configuration reference (will move into the trunk when done)
 * </li>
 * <li> https://www.home-assistant.io/integrations/mqtt#mqtt-discovery HA MQTT Discovery reference</li>
 * <li> https://www.home-assistant.io/integrations/climate.mqtt/ HA MQTT HVAC component</li>
 * <li> https://www.home-assistant.io/integrations/climate/ HA Climate component</li>
 * <li> https://esphome.io/components/climate/index.html ESPHome Climate component</li>
 * </ul>
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class HomeAssistantConnector implements Connector {

    private final Logger logger = LogManager.getLogger();

    private final Config config;
    private final MqttAdapter mqttAdapter;
    private final Set<Zone> zonesConfigured;

    private final Map<Zone, HvacMode> zone2mode = new LinkedHashMap<>();
    private final Map<Zone, ZoneDiscoveryPacket> zone2meta = new LinkedHashMap<>();

    private ObjectMapper objectMapper;
    private final TopicResolver topicResolver = new TopicResolver();

    public HomeAssistantConnector(String version, String id, MqttAdapter mqttAdapter, String discoveryPrefix, Set<Zone> zonesConfigured) {
        this.config = new Config(version, id, discoveryPrefix);
        this.mqttAdapter = mqttAdapter;
        this.zonesConfigured = Collections.unmodifiableSet(zonesConfigured);
    }

    @Override
    public void close() throws Exception {

        // LWT is not of much use.
        // Problem is, it's per connection, while here the connection is shared, and
        // different zones may become unavailable because of sensor failures.

        zone2meta.forEach((zone, meta) -> mqttAdapter.publish(
                topicResolver.resolve(meta.rootTopic, meta.availabilityTopic),
                meta.payloadNotAvailable,
                MqttQos.AT_MOST_ONCE,
                false
        ));

        logger.info("done");
    }

    @Override
    public void connect(String unitId, UnitDirector.Feed feed) {

        ThreadContext.push("connect");
        try {

            var sensorFlux2zone = getExposedZones(feed.sensorFlux2zone);

            Flux
                    .fromIterable(sensorFlux2zone.entrySet())
                    // No need to wait for this, and we can make it parallel (even though MQTT will likely
                    // eat some of the parallelism)

                    .parallel()
                    .runOn(Schedulers.boundedElastic())

                    .flatMap(this::announce)
                    .doOnNext(this::broadcast)
                    .subscribe(this::receive);

            feed.hvacDeviceFlux
                    .doOnNext(s -> {
                        if (s.getValue().command.mode == null) {
                            logger.debug("null hvacMode (normal on startup): {}", s);
                        }
                    })
                    .filter(s -> s.getValue().command.mode != null)
                    .map(s -> new Signal<HvacMode, String>(s.timestamp, s.getValue().command.mode, unitId, s.status, s.error))
                    .subscribe(mode -> captureMode(mode, sensorFlux2zone.values()));

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Render and send the discovery packet.
     *
     * @param source Zone to announce (ignore the flux for now).
     */
    private Flux<ZoneTuple> announce(Map.Entry<Flux<Signal<Double, Void>>, Zone> source) {

        var signal = source.getKey();
        var zone = source.getValue();

        ThreadContext.push("announce: " + zone.getAddress());

        try {

            var errorCounter =
                    checkCharacters("config.id", config.id)
                            + checkCharacters("zones.name", zone.getAddress());

            if (errorCounter > 0) {
                logger.error("zone skipped: {}", zone.getAddress());
                return Flux.empty();
            }

            var configTopic =
                    config.discoveryPrefix
                            + "/climate/"
                            // It's recommended to skip the node_id level, so there
                            + config.id + "-" + zone.getAddress()
                            + "/config";
            var root = "/hcc/ha-connector/" + config.id
                    + "/" + zone.getAddress();

            logger.debug("MQTT endpoint: {}", mqttAdapter.address);
            logger.debug("config topic: {}", configTopic);

            var uniqueId = config.id + "-" + zone.getAddress();
            var discoveryPacket = new ZoneDiscoveryPacket(
                    root,
                    zone.getAddress(),
                    // VT: FIXME: propagate the right values here
                    new String[] {"off", "cool"},
                    // VT: FIXME: propagate
                    20, 33,
                    uniqueId,
                    new DeviceDiscoveryPacket(
                        uniqueId,
                            "Home Climate Control",
                            "Zone: " + zone.getAddress(),
                            config.version,
                            "homeclimatecontrol.com"
                    ));

            try {

                // The payload gets logged at TRACE level, search for the config topic in the log to find it
                var payload = getObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(discoveryPacket);

                mqttAdapter.publish(configTopic, payload, MqttQos.AT_LEAST_ONCE, true);

            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Failed to convert materialized discoveryPacket to JSON", ex);
            }

            zone2meta.put(zone, discoveryPacket);

            // The discovery packet contains topic information that will come handy on the next step
            return Flux.just(new ZoneTuple(zone, signal, discoveryPacket));

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Connect the data sources to broadcast data to HA with MQTT topics in the discovery packet.
     */
    private void broadcast(ZoneTuple source) {

        // Establish an "alive" broadcast
        Flux
                .interval(
                        Duration.ZERO,
                        Duration.ofMinutes(1))
                        .subscribe(ignore -> mqttAdapter.publish(
                                topicResolver.resolve(source.meta.rootTopic, source.meta.availabilityTopic),
                                source.meta.payloadAvailable,
                                MqttQos.AT_MOST_ONCE,
                                false
                        ));

        // Establish a "status" broadcast

        source.signal
                .filter(Predicate.not(Signal::isError))
                .subscribe(s -> broadcast(
                        topicResolver.resolve(
                                source.meta.rootTopic,
                                source.meta.currentTemperatureTopic),
                        s.getValue(),
                        source.zone.getSettings().enabled,
                        zone2mode.get(source.zone),
                        source.zone.getSettings().setpoint));

        logger.error("{}: FIXME: broadcast", source.zone.getAddress());
    }

    private void broadcast(String topic, Double currentTemperature, Boolean enabled, HvacMode mode, Double setpoint) {

        // Operating mode is a bit of a challenge; zone's OFF state will translate into OFF state for the whole control

        String finalMode;

        if (Boolean.TRUE.equals(enabled)) {
            finalMode = Optional.ofNullable(mode).map(m -> m == HvacMode.COOLING ? "cool" : "heat").orElse("off");
        } else {
            finalMode = "off";
        }

        var message = new StateMessage(
                currentTemperature,
                finalMode,
                setpoint);

        logger.debug("broadcast: {}", message);

        try {
            var payload =  getObjectMapper()
                    .writeValueAsString(message);
            mqttAdapter.publish(topic, payload, MqttQos.AT_MOST_ONCE, false);
        } catch (JsonProcessingException ex) {
            logger.error("Failed to render JSON from {}", message, ex);
        }
    }

    /**
     * Record the mode information for exposed zones so tht {@link #broadcast(ZoneTuple)} can send it out.
     *
     * @param mode Mode to record.
     * @param zones Zones to associate it with
     */
    private void captureMode(Signal<HvacMode, String> mode, Collection<Zone> zones) {

        if (mode.isError()) {
            logger.warn("captureMode: don't know what to do with error mode: {}", mode);
        }

        zones.forEach(z -> zone2mode.put(z, mode.getValue()));
    }

    /**
     * Syntax sugar: strip unnecessary parts of the argument and pass it to the actual call.
     */
    private void receive(ZoneTuple source) {

        // The syntax up the call stack looked too nice to spoil it with extracting what we *really* need :)

        receive(
                source.zone,
                topicResolver.resolve(
                        source.meta.rootTopic,
                        source.meta.modeCommandTopic),
                topicResolver.resolve(
                        source.meta.rootTopic,
                        source.meta.temperatureCommandTopic));
    }

    /**
     * Connect the MQTT topics for receiving commands from HA to controls that make it happen.
     *
     * @param zone Zone to control
     * @param modeCommandTopic Topic to accept commands to switch between "off" and current operating mode.
     * @param temperatureCommandTopic Topic to accept setpoint change commands.
     */
    private void receive(Zone zone, String modeCommandTopic, String temperatureCommandTopic) {

        // These come published on their own schedulers
        mqttAdapter
                .getFlux(modeCommandTopic, false)
                .map(MqttSignal::message)
                .map(message -> setMode(zone, message))
                .subscribe(result -> logger.info("{}: setMode: {}", zone.getAddress(), result));
    }

    private String setMode(Zone zone, String command) {

        var enabled = !"off".equals(command);
        var currentSettings = zone.getSettings();
        var newSettings = new ZoneSettings(currentSettings, enabled);

        zone.setSettingsSync(newSettings);

        return command;
    }

    private final Pattern pattern = Pattern.compile("[^A-Za-z0-9_-]");

    /**
     * @return 0 if there were no disallowed characters, 1 otherwise
     */
    private int checkCharacters(String target, String value) {

        if (pattern.matcher(value).find()) {
            logger.error("{} contains characters outside of allowed [A-Za-z0-9_-] range: {}", target, value);
            return 1;
        }

        return 0;
    }

    /**
     * Get the set of zones that will be actually exposed.
     *
     * @param source Zone mappings provided by the {@link net.sf.dz3r.model.UnitDirector.Feed}.
     *
     * @return Zone mappings that will actually be exposed.
     */
    private Map<Flux<Signal<Double, Void>>, Zone> getExposedZones(Map<Flux<Signal<Double, Void>>, Zone> source) {

        // Need to make a copy, retainAll() will modify this
        var result = new LinkedHashMap<Flux<Signal<Double, Void>>, Zone>();

        logger.debug("connected zones ({} total):", source.size());
        for (var kv : source.entrySet()) {

            var zone = kv.getValue();
            logger.debug("  {}", zone.getAddress());

            if (zonesConfigured.contains(zone)) {
                result.put(kv.getKey(), zone);
            }
        }

        logger.info("exposed zones ({} total):", result.size());
        for (var kv : result.entrySet()) {
            logger.info("  {}", kv.getValue().getAddress());
        }

        if (result.size() < zonesConfigured.size()) {

            var leftovers = new TreeSet<>(zonesConfigured);
            leftovers.removeAll(result.values());

            logger.warn("not all configured zones were exposed, here's what's left ({} total):", leftovers.size());
            for (var z : leftovers) {
                logger.warn("  {}", z.getAddress());
            }
        }

        return result;
    }

    private synchronized ObjectMapper getObjectMapper() {

        if (objectMapper == null) {

            objectMapper = new ObjectMapper();

            // Necessary to print Optionals in a sane way
            objectMapper.registerModule(new Jdk8Module());

            // Necessary to deal with Duration
            objectMapper.registerModule(new JavaTimeModule());

            // For Quarkus to deal with interfaces nicer
            objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

            // For standalone to allow to ignore the root element
            objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        }

        return objectMapper;
    }

    private record Config(
            String version,
            String id,
            String discoveryPrefix
    ) {

    }

    private record ZoneTuple(
            Zone zone,
            Flux<Signal<Double, Void>> signal,
            ZoneDiscoveryPacket meta
    ) {

    }

    /**
     * Message to send up the MQTT stream.
     */
    private record StateMessage(
            @JsonProperty("current_temperature")
            Double currentTemperature,
            String mode,
            Double setpoint
    ) {

    }

    static class TopicResolver {

        /**
         * Resolve the topic absolute name.
         *
         * @param rootTopic Device root topic (see {@link ZoneDiscoveryPacket#rootTopic}.
         * @param topic Abbreviate topic name (with {@code ~} instead of the root topic.
         *
         * @return Absolute topic name.
         */
        public String resolve(String rootTopic, String topic) {
            return topic.replace("~", rootTopic);
        }
    }
}
