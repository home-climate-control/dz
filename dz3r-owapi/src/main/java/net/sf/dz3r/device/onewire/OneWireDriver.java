package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import net.sf.dz3r.common.IntegerChannelAddress;
import net.sf.dz3r.device.actuator.NullSwitch;
import net.sf.dz3r.device.actuator.Switch;
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
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
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

        devicesPresent.add(((OneWireNetworkArrival) event).address);
    }

    private void handleDeparture(OneWireNetworkEvent event) {

        // VT: FIXME: Reimplement this as a subscriber
        if (!(event instanceof OneWireNetworkDeparture)) {
            return;
        }

        devicesPresent.remove(((OneWireNetworkDeparture) event).address);
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

    public Switch<String> getSwitch(String address) {
        logger.info("getSwitch: {}", address);

        checkSwitchAddress(address);

        // With the sensor, we can just dole out a flux and let them wait.
        // Here, need something similar - nothing may be available at this point, but they still need it
        // (worse, they might just try to issue commands right away).

        return new SwitchProxy(address);
    }

    private void checkSwitchAddress(String address) {
        new IntegerChannelAddress(address);
        // Syntax is OK if we made it this far, let's proceed
    }

    public class SwitchProxy implements Switch<String> {

        private final String address;

        private final NullSwitch nullSwitch;

        /**
         * Monitor acquisition gate.
         *
         * Once the monitor is available, we don't need this anymore, so one use disposable semaphore.
         */
        private final CountDownLatch gate = new CountDownLatch(1);

        public SwitchProxy(String address) {
            this.address = address;
            this.nullSwitch = new NullSwitch(address);

            new Thread(() -> {
                ThreadContext.push("switchProxy:" + address);
                var m = new Marker("switchProxy:" + address);

                try {

                    // Need to get the monitor first

                    if (monitor == null) {
                        logger.debug("No 1-Wire monitor, getting one");
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

        @Override
        public Flux<Signal<State, String>> getFlux() {

            getMonitor("getFlux");
            return nullSwitch.getFlux();
        }

        private void getMonitor(String marker) {

            if (gate.getCount() == 0) {
                return;
            }

            logger.debug("{}({}): waiting for 1-Wire  monitor to become available", marker, address);

            try {
                gate.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for 1-Wire  monitor for " + address, ex);
            }

            logger.debug("{}({}): 1-Wire monitor ready", marker, address);
        }

        @Override
        public Mono<Boolean> setState(boolean state) {

            getMonitor("setState");
            return nullSwitch.setState(state);
        }

        @Override
        public Mono<Boolean> getState() {

            getMonitor("getState");
            return nullSwitch.getState();
        }
    }
}
