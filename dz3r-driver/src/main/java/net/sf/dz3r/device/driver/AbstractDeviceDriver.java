package net.sf.dz3r.device.driver;

import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
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
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Base class for device drivers.
 *
 * @param <A> Address type.
 * @param <T> Signal value type.
 * @param <P> Extra payload type.
 * @param <D> Adapter type
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public abstract class AbstractDeviceDriver<A extends Comparable<A>, T, P, D> implements SignalSource<A, T, P>, AutoCloseable {

    protected final Logger logger = LogManager.getLogger();

    /**
     * How long to wait before reporting a timeout.
     */
    protected Duration timeout = Duration.ofSeconds(30);

    protected final Clock clock = Clock.systemUTC();

    protected final Set<A> devicesPresent = Collections.synchronizedSet(new TreeSet<>());

    private Flux<DriverNetworkEvent> driverFlux;

    /**
     * Get the flux of readings from any device producing readings that can be interpreted as {@code T}.
     *
     * @param address Device address to get the flux of readings for.
     *
     * @return Flux of device readings. See {@link #getSensorsFlux()} for more details.
     */
    @Override
    public final Flux<Signal<T, P>> getFlux(A address) {

        logger.info("getFlux: {}", address);

        return new TimeoutGuard<T, P>(timeout)
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
    public final Mono<Boolean> isPresent(A address) {
        return Mono.just(devicesPresent.contains(address));
    }

    /**
     * Check device presence as a signal.
     *
     * @param address Address to check the presence of.
     *
     * @return Empty flux if the device is present, flux containing one {@link Signal.Status#FAILURE_TOTAL} signal if not.
     *
     * @see #getFlux(Comparable)
     */
    public final Mono<Signal<T, P>> checkPresence(A address) {
        return devicesPresent.contains(address)
                ? Mono.empty()
                : Mono.just(
                new Signal<>(
                        clock.instant(),
                        null,
                        (P) address,
                        Signal.Status.FAILURE_TOTAL,
                        new IllegalArgumentException(address + ": not present")));
    }

    /**
     * Get the flux of readings from all devices producing readings that can be interpreted as {@link Double}.
     *
     * @return Flux of device readings, with the device address as the {@link Signal#payload}.
     * If the device is not present at subscription time, or when the device departs, the flux will emit a
     * {@link Signal.Status#FAILURE_TOTAL} signal once, same in case when there isa failure reading from the device.
     * When the device returns (or is detected for the first time), the readings just start being emitted.
     *
     * This flux will only emit an error in case of an unrecoverable problem with the hardware adapter.
     */
    protected final Flux<Signal<T, P>> getSensorsFlux() {
        logger.debug("getSensorFlux()");
        return getDriverFlux()
                .flatMap(this::getSensorSignal);
    }

    /**
     * Parse a network event into a flux of sensor readings.
     *
     * @param event Event to parse.
     *
     * @return Flux of sensor readings (one network sample may contain more than one reading).
     */
    protected abstract Flux<Signal<T, P>> getSensorSignal(DriverNetworkEvent event);

    protected final synchronized Flux<DriverNetworkEvent> getDriverFlux() {

        if (driverFlux != null) {
            logger.debug("getDriverFlux(): existing");
            return driverFlux;
        }

        logger.debug("getDriverFlux(): new");
        driverFlux = Flux
                .create(this::connect)
                .doOnNext(this::handleArrival)
                .doOnNext(this::handleDeparture)
                .publish()
                .autoConnect();

        return driverFlux;
    }

    /**
     * Get the switch with no heartbeat support.
     *
     * It is highly recommended using {@link #getSwitch(A, long)} because of easy hardware bus flooding
     * with some producers.
     *
     * @param address Switch address.
     * @return The switch.
     */
    public Switch<A> getSwitch(A address) {
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
    public Switch<A> getSwitch(A address, long heartbeatSeconds) {

        if (heartbeatSeconds == 0) {
            logger.warn("getSwitch: {} (no heartbeat)", address);
            logger.warn("getSwitch: providing a heartbeat ensures your logs and hardware are not flooded, consider adding one");
        } else {
            logger.info("getSwitch: {} ({} seconds heartbeat)", address, heartbeatSeconds);

        }

        checkSwitchAddress(address);
        checkHeartbeat(heartbeatSeconds);

        // With the sensor, we can just dole out a flux and let them wait.
        // Here, need something similar - nothing may be available at this point, but they still need it
        // (worse, they might just try to issue commands right away).

        return getSwitchProxy(address, heartbeatSeconds);
    }

    private void checkHeartbeat(long heartbeatSeconds) {
        if (heartbeatSeconds < 0) {
            throw new IllegalArgumentException("heartbeatSeconds can't be negative (" + heartbeatSeconds + " given)");
        }
    }

    protected abstract void connect(FluxSink<DriverNetworkEvent> sink);
    protected abstract void handleArrival(DriverNetworkEvent event);
    protected abstract void handleDeparture(DriverNetworkEvent event);
    protected abstract SwitchProxy getSwitchProxy(A address, long heartbeatSeconds);
    protected abstract void checkSwitchAddress(A address);
    protected abstract DriverNetworkMonitor<D> getMonitor();

    public abstract class SwitchProxy implements Switch<A> {

        private final A address;
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

        protected SwitchProxy(A address, long heartbeatSeconds) {
            this.address = address;
            this.heartbeat = Duration.ofSeconds(heartbeatSeconds);

            new Thread(() -> {
                ThreadContext.push("switchProxy:" + address);
                var m = new Marker("switchProxy:" + address);

                logger.info("address={}, heartbeat={}", address, heartbeat);
                try {

                    // Need to get the monitor first

                    if (AbstractDeviceDriver.this.getMonitor() == null) {
                        logger.debug("No network monitor, getting one");

                        // This guarantees the monitor availability, but not the device presence
                        getDriverFlux().blockFirst();

                        logger.debug("Obtained the network monitor");
                        gate.countDown();
                    }

                } finally {
                    m.close();
                    ThreadContext.pop();
                }
            }).start();
        }

        @Override
        public final A getAddress() {
            return address;
        }

        private DriverNetworkMonitor<D> getMonitor(String marker) {

            if (gate.getCount() == 0) {
                return AbstractDeviceDriver.this.getMonitor();
            }

            logger.debug("{}({}): waiting for network  monitor to become available", marker, address);

            try {
                gate.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for network  monitor for " + address, ex);
            }

            logger.debug("{}({}): network monitor ready", marker, address);
            return AbstractDeviceDriver.this.getMonitor();
        }

        @Override
        public Flux<Signal<Switch.State, String>> getFlux() {

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

                var commandSink = getMonitor("setState").getCommandSink();
                var messageId = UUID.randomUUID();
                commandSink.next(getSetSwitchCommand(address, commandSink, messageId, state));

                return expectSwitchState(messageId);

            } finally {
                requested = state;
                requestedAt = Instant.now();
            }
        }

        protected abstract Mono<Boolean> expectSwitchState(UUID messageId);
        protected abstract DriverCommand<D> getSetSwitchCommand(A address, FluxSink<DriverCommand<D>> commandSink, UUID messageId, boolean state);

        @Override
        public Mono<Boolean> getState() {

            getMonitor("getState");
            throw new UnsupportedOperationException("Not Implemented");
        }
    }
}
