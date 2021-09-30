package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import net.sf.dz3r.device.onewire.event.OneWireNetworkArrival;
import net.sf.dz3r.device.onewire.event.OneWireNetworkDeparture;
import net.sf.dz3r.device.onewire.event.OneWireNetworkEvent;
import net.sf.dz3r.device.onewire.event.OneWireNetworkTemperatureSample;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Driver for 1-Wire subsystem using the <a href="https://github.com/home-climate-control/owapi-reborn">owapi-reborn</a> library.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireDriver implements SignalSource<String, Double, String> {

    private final Logger logger = LogManager.getLogger();
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
        return Flux.concat(
                checkPresence(address),
                getSensorsFlux()
                        .filter(s -> address.equals(s.payload)));
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

        this.monitor = new OneWireNetworkMonitor(endpoint, sink);
    }
}
