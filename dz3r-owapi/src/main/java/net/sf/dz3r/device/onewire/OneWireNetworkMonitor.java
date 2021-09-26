package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.adapter.USerialAdapter;
import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3r.device.onewire.command.OneWireCommand;
import net.sf.dz3r.device.onewire.command.OneWireCommandReadTemperatureAll;
import net.sf.dz3r.device.onewire.command.OneWireCommandRescan;
import net.sf.dz3r.device.onewire.event.OneWireNetworkArrival;
import net.sf.dz3r.device.onewire.event.OneWireNetworkDeparture;
import net.sf.dz3r.device.onewire.event.OneWireNetworkEvent;
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

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class OneWireNetworkMonitor {

    private final Logger logger = LogManager.getLogger();

    private final Duration rescanInterval = Duration.ofSeconds(30);
    private final Duration readTemperatureInterval = Duration.ofSeconds(5);

    private final OneWireEndpoint endpoint;
    private final FluxSink<OneWireNetworkEvent> observer;
    private FluxSink<OneWireCommand> commandSink;
    private Disposable commandSubscription;

    private static AtomicBoolean gate = new AtomicBoolean(false);

    /**
     * 1-Wire adapter.
     */
    private DSPortAdapter adapter = null;

    private final Set<String> devicesPresent = Collections.synchronizedSet(new TreeSet<>());

    public OneWireNetworkMonitor(OneWireEndpoint endpoint, FluxSink<OneWireNetworkEvent> observer) {

        if (!gate.compareAndSet(false, true)) {
            throw new IllegalStateException("Constructor called more than once, coding error, submit a report with this stack trace");
        }

        this.endpoint = endpoint;
        this.observer = observer;

        // Start the rescan immediately
        var rescanFlux = Flux
                .interval(Duration.ZERO, rescanInterval)
                .map(l -> new OneWireCommandRescan(commandSink, new TreeSet<>(devicesPresent)));

        // Let's read the temperature shortly after the rescan - it will be executed immediately anyway
        // because rescan is likely to take longer
        var readTemperatureFlux = Flux
                .interval(Duration.ofMillis(100), readTemperatureInterval)
                .map(l -> new OneWireCommandReadTemperatureAll(commandSink));

        var externalCommandFlux = Flux.create(this::connect);

        commandSubscription = Flux
                .merge(rescanFlux, readTemperatureFlux, externalCommandFlux)
                .publishOn(Schedulers.newSingle("1-Wire command"))
                .doOnNext(c -> logger.info("command: {}", c))
                .flatMap(this::execute)
                .doOnNext(this::handleOneWireEvent)
                .doOnNext(this::broadcastOneWireEvent)
                .subscribe();
    }

    private void connect(FluxSink<OneWireCommand> commandSink) {
        this.commandSink = commandSink;
    }

    private Flux<OneWireNetworkEvent> execute(OneWireCommand command) {
        ThreadContext.push("execute");
        try {

            return command.execute(getAdapter(), command);

        } catch (Throwable t) {
            logger.error("1-Wire command execution failed: {}", command, t);

            // VT: FIXME: No response for now, but we'll need to propagate error status
            return Flux.empty();

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Get the adapter to talk to 1-Wire hardware through.
     *
     * @return A fully initialized adapter.
     */
    private DSPortAdapter getAdapter() throws IOException, OneWireException {

        if (adapter != null) {
            return adapter;
        }

        ThreadContext.push("getAdapter");
        Marker m = new Marker("getAdapter");
        try {


            DSPortAdapter newAdapter;
            boolean ok;
            try {

                newAdapter = new USerialAdapter();

                // VT: NOTE: Having succeeded at selecting the port doesn't necessarily mean that we'll be fine.
                // Serial based adapters don't seem to be accessed during selectPort(), and it's quite
                // possible to successfully select a port that doesn't correspond to an existing adapter.
                // Additional test is required to make sure we're OK.

                ok = newAdapter.selectPort(endpoint.port);

            } catch (OneWireException ex) {
                logger.error("Failed to open port '{}'", endpoint.port, ex);
                ok = false;
                newAdapter = null;
            }

            if (!ok) {

                throw new IllegalArgumentException("Port '" + endpoint.port + "' unavailable, valid values: "
                        + DSPortAdapter.getPortNames() + "\n"
                        + "Things to check:\n"
                        + "    http://stackoverflow.com/questions/9628988/ubuntu-rxtx-does-not-recognize-usb-serial-device yet?");
            }

            try {

                // Now, *this* should take care of it...
                newAdapter.reset();

            } catch (OneWireIOException ex) {

                if ("Error communicating with adapter".equals(ex.getMessage())) {
                    throw new IOException("Port '" + endpoint.port
                            + "' doesn't seem to have adapter connected, check others: " + DSPortAdapter.getPortNames(), ex);
                }
            }

            logger.info("Adapter port: {}", endpoint.port);

            try {

                logger.info("Setting adapter speed to {}", endpoint.speed);
                newAdapter.setSpeed(endpoint.speed);

            } catch (Throwable t) {
                // Not fatal, can continue
                logger.error("Failed to set adapter speed, cause:", t);
            }

            this.adapter = newAdapter;

            return adapter;

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void handleOneWireEvent(OneWireNetworkEvent event) {

        switch (event.getClass().getSimpleName()) {
            case "OneWireNetworkArrival":
                devicesPresent.add(((OneWireNetworkArrival) event).address);
                logger.info("arrival: acknowledged {}", ((OneWireNetworkArrival) event).address);
                break;
            case "OneWireNetworkDeparture":
                devicesPresent.remove(((OneWireNetworkDeparture) event).address);
                logger.info("departure: acknowledged {}", ((OneWireNetworkDeparture) event).address);
                break;
            default:
                logger.info("Not handling {} event yet", event);
        }
    }

    private void broadcastOneWireEvent(OneWireNetworkEvent event) {
        observer.next(event);
    }
}
