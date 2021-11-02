package net.sf.dz3r.device.driver;

import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import net.sf.dz3r.signal.filter.TimeoutGuard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base class for device drivers.
 *
 * @param <A> Address type.
 * @param <T> Signal value type.
 * @param <P> Extra payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public abstract class AbstractDeviceDriver<A extends Comparable<A>, T, P> implements SignalSource<A, T, P> {

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
        logger.info("getSensorFlux()");
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
        logger.info("getDriverFlux()");

        if (driverFlux != null) {
            return driverFlux;
        }

        driverFlux = Flux
                .create(this::connect)
                .doOnNext(this::handleArrival)
                .doOnNext(this::handleDeparture)
                .publish()
                .autoConnect();

        return driverFlux;
    }

    protected abstract void connect(FluxSink<DriverNetworkEvent> sink);
    protected abstract void handleArrival(DriverNetworkEvent event);
    protected abstract void handleDeparture(DriverNetworkEvent event);
}
