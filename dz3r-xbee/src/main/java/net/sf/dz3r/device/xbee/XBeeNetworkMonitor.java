package net.sf.dz3r.device.xbee;

import com.homeclimatecontrol.xbee.XBeeReactive;
import net.sf.dz3r.device.xbee.command.XBeeCommand;
import net.sf.dz3r.device.xbee.command.XBeeCommandRescan;
import net.sf.dz3r.device.xbee.event.XBeeNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class XBeeNetworkMonitor implements AutoCloseable {

    private final Logger logger = LogManager.getLogger();
    private final Duration rescanInterval = Duration.ofSeconds(60);
    private final Set<String> devicesPresent = Collections.synchronizedSet(new TreeSet<>());

    private final FluxSink<XBeeNetworkEvent> observer;
    private FluxSink<XBeeCommand> commandSink;
    private Disposable commandSubscription;
    private static final AtomicBoolean gate = new AtomicBoolean(false);

    private final XBeeReactive xbee;


    public XBeeNetworkMonitor(String port, FluxSink<XBeeNetworkEvent> observer) throws IOException {

        if (!gate.compareAndSet(false, true)) {
            throw new IllegalStateException("Constructor called more than once, coding error, submit a report with this stack trace");
        }

        this.observer = observer;
        this.xbee = new XBeeReactive(port);

        // Connect the command flux first
        var externalCommandFlux = Flux.create(this::connect);

        // Start the rescan immediately
        var rescanFlux = Flux
                .interval(Duration.ZERO, rescanInterval)
                .map(l -> new XBeeCommandRescan(commandSink, new TreeSet<>(devicesPresent)));

        commandSubscription = Flux
                .merge(externalCommandFlux, rescanFlux)

                // Critical section - rather not send more than one command to XBee at a time, it can't handle it anyway
                .publishOn(Schedulers.newSingle("XBee command"))
                .doOnNext(c -> logger.debug("XBee command: {}", c))
                .flatMap(this::execute)
                .doOnNext(e -> logger.debug("XBee event: {}", e))

                // Out of critical section - we're not touching hardware anymore
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::handleXBeeEvent)
                .doOnNext(this::broadcastXBeeEvent)
                .subscribe();
    }

    private void handleXBeeEvent(XBeeNetworkEvent event) {
    }

    private void broadcastXBeeEvent(XBeeNetworkEvent event) {
    }

    private void connect(FluxSink<XBeeCommand> commandSink) {
        this.commandSink = commandSink;
    }

    private Flux<XBeeNetworkEvent> execute(XBeeCommand command) {
        ThreadContext.push("execute");
        try {

            return command.execute(xbee, command);

        } catch (Throwable t) {
            logger.error("1-Wire command execution failed: {}", command, t);

            // VT: FIXME: No response for now, but we'll need to propagate error status
            return Flux.empty();

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public void close() throws Exception {
        commandSubscription.dispose();
    }
}
