package net.sf.dz3r.device.xbee;

import com.homeclimatecontrol.xbee.AddressParser;
import com.homeclimatecontrol.xbee.XBeeReactive;
import com.homeclimatecontrol.xbee.response.frame.IOSampleIndicator;
import com.homeclimatecontrol.xbee.response.frame.LocalATCommandResponse;
import com.homeclimatecontrol.xbee.response.frame.XBeeResponseFrame;
import com.rapplogic.xbee.api.AtCommand;
import net.sf.dz3r.device.driver.DriverNetworkMonitor;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.xbee.command.XBeeCommandRescan;
import net.sf.dz3r.device.xbee.event.XBeeNetworkArrival;
import net.sf.dz3r.device.xbee.event.XBeeNetworkIOSample;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.rapplogic.xbee.api.AtCommand.Command.AP;

public class XBeeNetworkMonitor extends DriverNetworkMonitor<XBeeReactive> implements AutoCloseable {

    private final Disposable commandSubscription;
    private final Disposable xbeeSubscription;

    private final XBeeReactive adapter;

    private final Map<String, Instant> lastSeen = Collections.synchronizedMap(new TreeMap<>());

    public XBeeNetworkMonitor(String port, FluxSink<DriverNetworkEvent> observer) {

        super(Duration.ofSeconds(60), observer);

        try {

            adapter = new XBeeReactive(port);

            API2(adapter);

        } catch (IOException e) {
            // Not much we can reasonably do at this point
            throw new IllegalStateException("Failed to get XBee adapter on " + port, e);
        }

        // Connect the command flux first
        var externalCommandFlux = Flux.create(this::connect);

        // Start the rescan immediately
        var rescanFlux = Flux
                .interval(Duration.ZERO, rescanInterval)
                .map(l -> new XBeeCommandRescan(getCommandSink(), new TreeSet<>(devicesPresent)));

        commandSubscription = Flux
                .merge(externalCommandFlux, rescanFlux)

                // XBee itself will take care of sending commands in FIFO order, and parsing responses
                .publishOn(Schedulers.boundedElastic())

                .doOnNext(c -> logger.debug("XBee command: {}", c))
                .flatMap(this::execute)
                .doOnNext(e -> logger.debug("XBee event: {}", e))
                .doOnNext(this::handleEvent)
                .doOnNext(this::broadcastEvent)
                .subscribe();

        // Unlike 1-Wire devices that need to be polled, XBee devices (in DZ) are configured to broadcast samples,
        // all we need to do is to listen to them and process those we need
        xbeeSubscription = getAdapter()
                .receive()
                .doOnNext(event -> handleXBeeEvent(event, observer))
                .subscribe();
    }

    private void API2(XBeeReactive adapter) throws IOException {

        var response = adapter.sendAT(new AtCommand(AP, 2), Duration.ofSeconds(5)).block();

        if (response == null) {
            throw new IOException("null response to API2");
        }

        if (!response.status.equals(LocalATCommandResponse.Status.OK)) {
            throw new IOException("Couldn't set API2: " + response);
        }
    }

    @Override
    protected void handleEvent(DriverNetworkEvent event) {

        switch (event.getClass().getSimpleName()) { // NOSONAR Just wait a bit
            case "XBeeNetworkArrival":
                handleArrival((XBeeNetworkArrival) event);
                break;
            default:
                logger.debug("Not handling {} ({}) event yet", event.getClass().getSimpleName(), event);
        }
    }

    private void handleXBeeEvent(XBeeResponseFrame event, FluxSink<DriverNetworkEvent> observer) {
        logger.debug("XBee event: {} {}", event.getClass().getName(), event);
        switch (event.getClass().getSimpleName()) { // NOSONAR Just wait a bit
            case "ZNetRxIoSampleResponse":
                handleIOSample((IOSampleIndicator) event, observer);
                break;
            default:
                logger.debug("Not handling {} ({}) event yet", event.getClass().getSimpleName(), event);
        }
    }

    private void handleIOSample(IOSampleIndicator event, FluxSink<DriverNetworkEvent> observer) {
        observer.next(new XBeeNetworkIOSample(Instant.now(), AddressParser.render4x4(event.sourceAddress64), event));
    }

    private void handleArrival(XBeeNetworkArrival event) {
        var address = event.address;

        // This will only add the *device* address, but not individual channel address.
        devicesPresent.add(address);
        lastSeen.put(event.address, event.timestamp);

        logger.info("arrival: acknowledged {}", address);
    }

    @Override
    protected XBeeReactive getAdapter() {
        return adapter;
    }

    @Override
    public void close() {
        commandSubscription.dispose();
        xbeeSubscription.dispose();
    }
}
