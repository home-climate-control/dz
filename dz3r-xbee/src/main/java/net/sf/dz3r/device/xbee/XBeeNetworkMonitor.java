package net.sf.dz3r.device.xbee;

import com.homeclimatecontrol.xbee.XBeeReactive;
import net.sf.dz3r.device.driver.DriverNetworkMonitor;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.xbee.command.XBeeCommandRescan;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.TreeSet;

public class XBeeNetworkMonitor extends DriverNetworkMonitor<XBeeReactive> implements AutoCloseable {


    private final Disposable commandSubscription;


    private final String port;
    private XBeeReactive adapter;

    public XBeeNetworkMonitor(String port, FluxSink<DriverNetworkEvent> observer) {

        super(Duration.ofSeconds(60), observer);

        this.port = port;

        // Connect the command flux first
        var externalCommandFlux = Flux.create(this::connect);

        // Start the rescan immediately
        var rescanFlux = Flux
                .interval(Duration.ZERO, rescanInterval)
                .map(l -> new XBeeCommandRescan(getCommandSink(), new TreeSet<>(devicesPresent)));

        commandSubscription = Flux
                .merge(externalCommandFlux, rescanFlux)

                // Critical section - rather not send more than one command to XBee at a time, it can't handle it anyway
                .publishOn(Schedulers.newSingle("XBee command"))
                .doOnNext(c -> logger.debug("XBee command: {}", c))
                .flatMap(this::execute)
                .doOnNext(e -> logger.debug("XBee event: {}", e))

                // Out of critical section - we're not touching hardware anymore
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::handleEvent)
                .doOnNext(this::broadcastEvent)
                .subscribe();
    }

    @Override
    protected void handleEvent(DriverNetworkEvent event) {
    }

    @Override
    public void close() throws Exception {
        commandSubscription.dispose();
    }

    @Override
    protected XBeeReactive getAdapter() throws IOException {

        if (adapter == null) {
            adapter = new XBeeReactive(port);
        }

        return adapter;
    }
}
