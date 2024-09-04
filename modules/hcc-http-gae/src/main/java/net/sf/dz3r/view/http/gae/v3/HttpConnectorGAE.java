package net.sf.dz3r.view.http.gae.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeclimatecontrol.hcc.model.ZoneSettings;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.http.gae.v3.wire.ZoneCommand;
import net.sf.dz3r.view.http.gae.v3.wire.ZoneSnapshot;
import net.sf.dz3r.view.http.v3.HttpConnector;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class HttpConnectorGAE extends HttpConnector {

    private final Logger logger = LogManager.getLogger();
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected HttpClient httpClient;

    private final Set<String> zoneNames;
    private final Map<String, Zone> name2zone = new TreeMap<>();

    private final Duration pollInterval = Duration.of(10, ChronoUnit.SECONDS);

    private final ZoneRenderer zoneRenderer = new ZoneRenderer();

    private final Sinks.Many<ZoneSnapshot> bufferSink = Sinks.many().unicast().onBackpressureBuffer();
    private final Disposable bufferSubscription;

    /**
     * Create an instance.
     *
     * @param serverContextRoot Server context root.
     * @param zoneNames Set of zone names to include into communications - some may be private.
     */
    public HttpConnectorGAE(URL serverContextRoot, Set<String> zoneNames) {
        super(serverContextRoot);
        this.zoneNames = new TreeSet<>(zoneNames);

        bufferSubscription = bufferSink.asFlux()
                .buffer(pollInterval)
                .doOnNext(this::exchange)
                .subscribe();
    }

    private synchronized HttpClient getHttpClient() {

        if (httpClient == null) {

            Marker m = new Marker("client creation");

            try {

                // VT: NOTE: This is about 100ms on a decent workstation. Unexpected.
                // ... but only if it is called from the constructor, otherwise it takes 2ms :O
                httpClient = HttpClientFactory.createClient();

            } finally {
                m.close();
            }
        }

        return httpClient;
    }

    @Override
    public void connect(String unitId, UnitDirector.Feed feed) {

        for (var zone : feed.sensorFlux2zone.values()) {
            logger.info("connected zone: {}", zone.getAddress());
            name2zone.put(zone.getAddress(), zone);
        }

        logger.info("{} zones total", name2zone.size());

        // Zones and zone controller have no business knowing about the sensor signal, but humans want it; inject it
        getAggregateSensorFlux(feed.sensorFlux2zone)
                .subscribe(zoneRenderer::consumeSensorSignal);

        // Zones and zone controller have no business knowing about HVAC mode; inject it
        feed.hvacDeviceFlux
                .doOnNext(s -> {
                    if (s.getValue().command.mode == null) {
                        logger.debug("null hvacMode (normal on startup): {}", s);
                    }
                })
                .filter(s -> s.getValue().command.mode != null)
                .map(s -> new Signal<HvacMode, String>(s.timestamp, s.getValue().command.mode, unitId, s.status, s.error))
                .subscribe(zoneRenderer::consumeMode);

        // Zone ("thermostat" in its terminology) status feed is the only one supported
        zoneRenderer.compute(
                        unitId,
                        feed.aggregateZoneFlux
                                .doOnNext(z -> logger.debug("Incoming zone: {}", z.payload))
                                .filter(z -> zoneNames.contains(z.payload))
                                .doOnNext(z -> logger.debug("Reportable zone: {}", z.payload))
                )
                .subscribe(bufferSink::tryEmitNext);
    }

    private Flux<Signal<Double, String>> getAggregateSensorFlux(Map<Flux<Signal<Double, Void>>, Zone> source) {

        var accumulator = new ArrayList<Flux<Signal<Double, String>>>(source.size());
        Flux.fromIterable(source.entrySet())
                .map(this::getSensorFlux)
                .subscribe(accumulator::add);

        return Flux.merge(accumulator);
    }

    private Flux<Signal<Double, String>> getSensorFlux(Map.Entry<Flux<Signal<Double, Void>>, Zone> source) {
        var zoneName = source.getValue().getAddress();
        return source
                .getKey()
                .map(s -> new Signal<>(s.timestamp, s.getValue(), zoneName, s.status, s.error));
    }

    private void exchange(List<ZoneSnapshot> buffer) {

        ThreadContext.push("exchange");
        var m = new Marker("exchange");

        try {

            logger.debug("sending {} items: {}", buffer.size(), buffer);

            var encoded = objectMapper.writeValueAsString(buffer);

            logger.debug("JSON ({} bytes): {}", encoded.length(), encoded);

            var targetUrl = serverContextRoot;
            var builder = new URIBuilder(targetUrl.toString());
            var post = new HttpPost(builder.toString());

            post.setHeader("HCC-Identity", getIdentity());
            post.setEntity(new StringEntity(encoded));

            try {

                var rsp = getHttpClient().execute(post);
                var rc = rsp.getStatusLine().getStatusCode();

                if (rc != 200) {

                    logger.error("HTTP rc={}, text follows:", rc);
                    logger.error(EntityUtils.toString(rsp.getEntity())); // NOSONAR Not worth the effort

                    throw new IOException("Request to " + targetUrl + " failed with HTTP code " + rc);
                }

                processResponse(EntityUtils.toString(rsp.getEntity()));

            } finally {
                post.releaseConnection();
            }

        } catch (Throwable t) { // NOSONAR Consequences have been considered

            // VT: NOTE: For now, this is not a recoverable problem, the snapshot is
            // irretrievably lost. Need to see if this matters at all

            logger.error("Buffer exchange failed", t);

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void processResponse(String response) throws JsonProcessingException {
        ThreadContext.push("processResponse");
        try {

            var buffer = objectMapper.readValue(response, new TypeReference<Set<ZoneCommand>>(){});
            logger.debug("Commands received ({}): {}", buffer.size(), buffer);

            Flux.fromIterable(buffer)
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(this::executeCommand);

        } finally {
            ThreadContext.pop();
        }
    }

    private void executeCommand(ZoneCommand command) {
        ThreadContext.push("executeCommand");
        try {

            // Input is not sanitized, need to do it here
            Optional
                    .ofNullable(name2zone.get(command.name))
                    .ifPresent(z -> z.setSettingsSync(
                            new ZoneSettings(
                                    command.enabled,
                                    command.setpointTemperature,
                                    command.voting,
                                    command.onHold,
                                    null,
                                    null)
                    ));

        } catch (Exception ex) {
            logger.error("failed to execute: {}", command, ex);
        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * @return Client identity to be sent to the proxy.
     */
    private String getIdentity() throws IOException, InterruptedException {

        ThreadContext.push("getIdentity");

        try {

            // VT: NOTE: This needs to be done every time because since last call,
            // either the access token expired (need to refresh),
            // or it could've been revoked (need to reacquire permissions

            var provider = new OAuth2DeviceIdentityProvider();

            var base = getSecretsDir();
            var identity = provider.getIdentity(
                    new File(base, "client-id"),
                    new File(base, "client-secret"),
                    new File(base, "token"),
                    "HttpConnector");

            logger.debug("identity: {}", identity);

            return identity;

        } finally {
            ThreadContext.pop();
        }
    }

    private File getSecretsDir() {

        var result = new File(System.getProperty("user.home"), ".dz/oauth/HttpConnector");

        if (!result.exists() || !result.isDirectory() || !result.canRead()) {
            throw new IllegalArgumentException(result + ": doesn't exist, not a directory, or can't read");
        }

        return result;
    }

    @Override
    public void close() throws Exception {
        ThreadContext.push("close");
        try {
            logger.warn("Shutting down: {}", serverContextRoot);
            bufferSubscription.dispose();
            logger.info("Shut down: {}", serverContextRoot);
        } finally {
            ThreadContext.pop();
        }
    }
}
