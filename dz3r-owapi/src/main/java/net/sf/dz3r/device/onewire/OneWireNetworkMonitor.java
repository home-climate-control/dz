package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.USerialAdapter;
import com.dalsemi.onewire.utils.OWPath;
import net.sf.dz3r.device.driver.DriverNetworkMonitor;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.onewire.command.OneWireCommandBumpResolution;
import net.sf.dz3r.device.onewire.command.OneWireCommandReadTemperatureAll;
import net.sf.dz3r.device.onewire.command.OneWireCommandRescan;
import net.sf.dz3r.device.onewire.event.OneWireNetworkArrival;
import net.sf.dz3r.device.onewire.event.OneWireNetworkDeparture;
import net.sf.dz3r.device.onewire.event.OneWireNetworkErrorEvent;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Watches over 1-Wire network, handles events, executes commands.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class OneWireNetworkMonitor extends DriverNetworkMonitor<DSPortAdapter> implements OWPathResolver {

    private final Duration readTemperatureInterval = Duration.ofSeconds(5);

    private final OneWireEndpoint endpoint;
    private final Disposable commandSubscription;

    /**
     * 1-Wire adapter.
     */
    private DSPortAdapter adapter = null;

    private final Map<String, OWPath> address2path = Collections.synchronizedMap(new TreeMap<>());

    public OneWireNetworkMonitor(OneWireEndpoint endpoint, FluxSink<DriverNetworkEvent> observer) {

        super(Duration.ofSeconds(30), observer);
        if (!gate.compareAndSet(false, true)) {
            throw new IllegalStateException("Constructor called more than once, coding error, submit a report with this stack trace");
        }

        this.endpoint = endpoint;

        // Connect the command flux first
        var externalCommandFlux = Flux.create(this::connect);

        // Start the rescan immediately
        var rescanFlux = Flux
                .interval(Duration.ZERO, rescanInterval)
                .map(l -> new OneWireCommandRescan(getCommandSink(), new TreeSet<>(devicesPresent)));

        // rescan will queue an extra read all command upon completion
        var readTemperatureFlux = Flux
                .interval(readTemperatureInterval)
                .map(l -> new OneWireCommandReadTemperatureAll(getCommandSink(), new TreeSet<>(devicesPresent), new TreeMap<>(address2path)));

        commandSubscription = Flux
                .merge(externalCommandFlux, rescanFlux, readTemperatureFlux)

                // Critical section - can't allow more than one thread to talk to the serial stream
                .publishOn(Schedulers.newSingle("1-Wire command"))
                .doOnNext(c -> logger.debug("1-Wire command: {}", c))
                .flatMap(this::execute)
                .doOnNext(e -> logger.debug("1-Wire event: {}", e))

                // Out of critical section - we're not touching hardware anymore
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::handleEvent)
                .doOnNext(this::broadcastEvent)
                .subscribe();
    }

    /**
     * Get the adapter to talk to 1-Wire hardware through.
     *
     * @return A fully initialized adapter.
     */
    @Override
    protected DSPortAdapter getAdapter() throws IOException {

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

            } catch (OneWireException ex) {

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

    @Override
    protected void handleEvent(DriverNetworkEvent event) {

        switch (event.getClass().getSimpleName()) {
            case "OneWireNetworkArrival":
                handleArrival((OneWireNetworkArrival) event);
                break;
            case "OneWireNetworkDeparture":
                handleDeparture((OneWireNetworkDeparture) event);
                break;
            case "OneWireNetworkErrorEvent":
                handleError((OneWireNetworkErrorEvent<?>) event);
                break;
            default:
                logger.debug("Not handling {} ({}) event yet", event.getClass().getSimpleName(), event);
        }
    }

    private void handleDeparture(OneWireNetworkDeparture event) {
        devicesPresent.remove(event.address);
        address2path.remove(event.address);
        logger.info("departure: acknowledged {}", event.address);
    }

    private void handleArrival(OneWireNetworkArrival event) {
        var address = event.address;
        devicesPresent.add(address);
        address2path.put(address, event.path);
        logger.info("arrival: acknowledged {} at {}", address, event.path);
        getCommandSink().next(new OneWireCommandBumpResolution(getCommandSink(), address, event.path));
    }

    private void handleError(OneWireNetworkErrorEvent<?> event) {
        logger.error("{}", event, event.error);
        logger.warn("Initiating 1-Wire network rescan");

        // It would be a good idea to rescan the bus to see what happened - but with a delay to prevent flooding
        new Thread(() -> {
            Flux.just(new OneWireCommandRescan(getCommandSink(), new TreeSet<>(devicesPresent)))
                    .delaySequence(Duration.ofSeconds(1))
                    .doOnNext(getCommandSink()::next)
                    .blockLast();
        }).start();
    }

    @Override
    public OWPath getPath(String address) {
        return address2path.get(address);
    }
}
