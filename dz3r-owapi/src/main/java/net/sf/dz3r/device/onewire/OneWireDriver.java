package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.utils.OWPath;
import net.sf.dz3r.common.IntegerChannelAddress;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.device.onewire.command.OneWireSetSwitchCommand;
import net.sf.dz3r.device.onewire.event.OneWireNetworkArrival;
import net.sf.dz3r.device.onewire.event.OneWireNetworkDeparture;
import net.sf.dz3r.device.onewire.event.OneWireNetworkEvent;
import net.sf.dz3r.device.onewire.event.OneWireNetworkTemperatureSample;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import net.sf.dz3r.signal.filter.TimeoutGuard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Driver for 1-Wire subsystem using the <a href="https://github.com/home-climate-control/owapi-reborn">owapi-reborn</a> library.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireDriver implements SignalSource<String, Double, String> {

    private final Logger logger = LogManager.getLogger();
    /**
     * How long to wait before reporting a timeout.
     */
    private Duration timeout = Duration.ofSeconds(30);

    private final Clock clock = Clock.systemUTC();

    private final OneWireEndpoint endpoint;

    private Flux<OneWireNetworkEvent> oneWireFlux;

    private OneWireNetworkMonitor monitor;

    private final Set<String> devicesPresent = Collections.synchronizedSet(new TreeSet<>());
    private final Map<String, OWPath> address2path = Collections.synchronizedMap(new TreeMap<>());

    /**
     * Create an instance working at default speed.
     *
     * @param adapterPort Port to use.
     */
    public OneWireDriver(String adapterPort) {
        this(adapterPort, DSPortAdapter.Speed.REGULAR);
    }

    /**
     * Create an instance.
     *
     * @param adapterPort Port to use.
     * @param adapterSpeed Speed to use (choices are "regular", "flex", "overdrive", "hyperdrive").
     */
    public OneWireDriver(String adapterPort, DSPortAdapter.Speed adapterSpeed) {
        this.endpoint = new OneWireEndpoint(adapterPort, adapterSpeed);
    }

    /**
     * Get the flux of readings from any device producing readings that can be interpreted as {@link Double}.
     *
     * One of most interest is the {@link com.dalsemi.onewire.container.TemperatureContainer}.
     *
     * @param address Address of the 1-Wire device to get the flux of readings for.
     *
     * @return Flux of device readings. See {@link #getSensorsFlux()} for more details.
     */
    @Override
    public Flux<Signal<Double, String>> getFlux(String address) {
        logger.info("getFlux: {}", address);
        return new TimeoutGuard<Double, String>(timeout)
                .compute(Flux.concat(
                checkPresence(address),
                getSensorsFlux()
                        .filter(s -> address.equals(s.payload))));
    }

    /**
     * Check device presence as a flag.
     *
     * @param address Address to check the presence of.
     *
     * @return A Mono with {@code true} if the device is present, and {@code false} if not.
     */
    public Mono<Boolean> isPresent(String address) {
        return Mono.just(devicesPresent.contains(address));
    }

    /**
     * Check device presence as a signal.
     *
     * @param address Address to check the presence of.
     *
     * @return Empty flux if the device is present, flux containing one {@link Signal.Status#FAILURE_TOTAL} signal if not.
     *
     * @see #getFlux(String)
     */
    public Mono<Signal<Double, String>> checkPresence(String address) {
        return devicesPresent.contains(address)
                ? Mono.empty()
                : Mono.just(
                new Signal<>(
                        clock.instant(),
                        null,
                        address,
                        Signal.Status.FAILURE_TOTAL,
                        new IllegalArgumentException(address + ": not present")));
    }

    /**
     * Get the flux of readings from all devices producing readings that can be interpreted as {@link Double}.
     *
     * One of most interest is the {@link com.dalsemi.onewire.container.TemperatureContainer}.
     *
     * @return Flux of device readings, with the device address as the {@link Signal#payload}.
     * If the device is not present at subscription time, or when the device departs, the flux will emit a
     * {@link Signal.Status#FAILURE_TOTAL} signal once, same in case when there isa failure reading from the device.
     * When the device returns (or is detected for the first time), the readings just start being emitted.
     *
     * This flux will only emit an error in case of an unrecoverable problem with the hardware adapter.
     */
    private Flux<Signal<Double, String>> getSensorsFlux() {
        logger.info("getSensorFlux()");
        return getOneWireFlux()
                .flatMap(this::getSensorSignal);
    }

    private Mono<Signal<Double, String>> getSensorSignal(OneWireNetworkEvent event) {

        if (!(event instanceof OneWireNetworkTemperatureSample)) {
            return Mono.empty();
        }

        var sample = (OneWireNetworkTemperatureSample) event;

        return Mono.just(new Signal<>(event.timestamp, sample.sample, sample.address));
    }

    private synchronized Flux<OneWireNetworkEvent> getOneWireFlux() {
        logger.info("getOneWireFlux()");

        if (oneWireFlux != null) {
            return oneWireFlux;
        }

        oneWireFlux = Flux
                .create(this::connect)
                .doOnNext(this::handleArrival)
                .doOnNext(this::handleDeparture)
                .publish()
                .autoConnect();

        return oneWireFlux;
    }

    private void handleArrival(OneWireNetworkEvent event) {

        // VT: FIXME: Reimplement this as a subscriber

        if (!(event instanceof OneWireNetworkArrival)) {
            return;
        }

        var arrivalEvent = (OneWireNetworkArrival) event;

        devicesPresent.add(arrivalEvent.address);
        address2path.put(arrivalEvent.address, arrivalEvent.path);
    }

    private void handleDeparture(OneWireNetworkEvent event) {

        // VT: FIXME: Reimplement this as a subscriber
        if (!(event instanceof OneWireNetworkDeparture)) {
            return;
        }

        var departureEvent = (OneWireNetworkDeparture) event;

        devicesPresent.remove(departureEvent.address);
        address2path.remove(departureEvent.address);

        logger.error("Departure not handled completely: {}", ((OneWireNetworkDeparture) event).address );
    }

    private FluxSink<OneWireNetworkEvent> sink;

    private void connect(FluxSink<OneWireNetworkEvent> sink) {

        if (this.sink != null) {
            throw new IllegalStateException("Fatal programming error, can't connect() more than once");
        }

        logger.info("Starting 1-Wire monitor for {}", endpoint);
        this.monitor = new OneWireNetworkMonitor(endpoint, sink);
    }

    /**
     * Get the switch with no heartbeat support.
     *
     * It is highly recommended to use {@link #getSwitch(String, long)} because of easy 1-Wire bus flooding
     * with some producers.
     *
     * @param address Switch address.
     * @return The switch.
     */
    public Switch<String> getSwitch(String address) {
        return getSwitch(address, 0);
    }

    /**
     * Get the switch with given heartbeat.
     *
     * @param address Switch address.
     * @param heartbeatSeconds Number of seconds to wait between sending identical state commands to hardware.
     *                         30 seconds is a reasonable default.
     * @return The switch.
     */
    public Switch<String> getSwitch(String address, long heartbeatSeconds) {
        logger.info("getSwitch: {}", address);

        checkSwitchAddress(address);
        checkHeartbeat(heartbeatSeconds);

        // With the sensor, we can just dole out a flux and let them wait.
        // Here, need something similar - nothing may be available at this point, but they still need it
        // (worse, they might just try to issue commands right away).

        return new SwitchProxy(address, heartbeatSeconds);
    }

    private void checkSwitchAddress(String address) {
        new IntegerChannelAddress(address);
        // Syntax is OK if we made it this far, let's proceed
    }

    private void checkHeartbeat(long heartbeatSeconds) {
        if (heartbeatSeconds < 0) {
            throw new IllegalArgumentException("heartbeatSeconds can't be negative (" + heartbeatSeconds + " given)");
        }
    }

    public class SwitchProxy implements Switch<String> {

        private final String address;
        private final Duration heartbeat;

        /**
         * Last requested state; {@code null} if {@link #setState(boolean)} hasn't been called yet.
         */
        private Boolean requested;

        /**
         * Timestamp of last {@link #setState(boolean)}; {@code null} if it hasn't been called yet.
         */
        private Instant requestedAt;

        /**
         * Monitor acquisition gate.
         *
         * Once the monitor is available, we don't need this anymore, so we're using a one time use disposable semaphore.
         */
        private final CountDownLatch gate = new CountDownLatch(1);

        public SwitchProxy(String address, long heartbeatSeconds) {
            this.address = address;
            this.heartbeat = Duration.ofSeconds(heartbeatSeconds);

            new Thread(() -> {
                ThreadContext.push("switchProxy:" + address);
                var m = new Marker("switchProxy:" + address);

                logger.info("address={}, heartbeat={}", address, heartbeat);
                try {

                    // Need to get the monitor first

                    if (monitor == null) {
                        logger.debug("No 1-Wire monitor, getting one");

                        // This guarantees the monitor availability, but not the device presence
                        getOneWireFlux().blockFirst();

                        logger.debug("Obtained the 1-Wire monitor");
                        gate.countDown();
                    }

                } finally {
                    m.close();
                    ThreadContext.pop();
                }
            }).start();
        }

        @Override
        public final String getAddress() {
            return address;
        }

        private OneWireNetworkMonitor getMonitor(String marker) {

            if (gate.getCount() == 0) {
                return monitor;
            }

            logger.debug("{}({}): waiting for 1-Wire  monitor to become available", marker, address);

            try {
                gate.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for 1-Wire  monitor for " + address, ex);
            }

            logger.debug("{}({}): 1-Wire monitor ready", marker, address);
            return monitor;
        }

        @Override
        public Flux<Signal<State, String>> getFlux() {

            getMonitor("getFlux");
            throw new UnsupportedOperationException("Not Implemented");
        }

        @Override
        public Mono<Boolean> setState(boolean state) {

            try {

                if (heartbeat.getSeconds() > 0 && requested != null && requested == state) {

                    var skip = Optional
                            .ofNullable(requestedAt)
                            .map(at -> Instant.now().isBefore(at.plus(heartbeat)))
                            .orElse(false);

                    if (skip) {
                        logger.debug("skipping setState({})={} - {} since last",
                                address, state, Duration.between(requestedAt, Instant.now()));
                        return Mono.just(state);
                    }
                }

                var channelAddress = new IntegerChannelAddress(address);
                var commandSink = getMonitor("setState").getCommandSink();
                commandSink.next(
                        new OneWireSetSwitchCommand(
                                UUID.randomUUID(),
                                commandSink,
                                monitor,
                                address,
                                address2path.get(channelAddress.hardwareAddress),
                                state));

                // VT: FIXME: This should actually return the result of getState() - but that'll be the next step.
                return Mono.just(state);

            } finally {
                requested = state;
                requestedAt = Instant.now();
            }
        }

        @Override
        public Mono<Boolean> getState() {

            getMonitor("getState");
            throw new UnsupportedOperationException("Not Implemented");
        }
    }
}
