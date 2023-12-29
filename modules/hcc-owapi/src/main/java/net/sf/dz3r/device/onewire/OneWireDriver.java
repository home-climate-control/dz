package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.utils.OWPath;
import net.sf.dz3r.common.IntegerChannelAddress;
import net.sf.dz3r.device.driver.AbstractDeviceDriver;
import net.sf.dz3r.device.driver.DriverNetworkMonitor;
import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.onewire.command.OneWireSetSwitchCommand;
import net.sf.dz3r.device.onewire.event.OneWireNetworkArrival;
import net.sf.dz3r.device.onewire.event.OneWireNetworkDeparture;
import net.sf.dz3r.device.onewire.event.OneWireNetworkTemperatureSample;
import net.sf.dz3r.device.onewire.event.OneWireSwitchState;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Driver for 1-Wire subsystem using the <a href="https://github.com/home-climate-control/owapi-reborn">owapi-reborn</a> library.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireDriver extends AbstractDeviceDriver<String, Double, String, DSPortAdapter> {

    private final OneWireEndpoint endpoint;

    private OneWireNetworkMonitor monitor;

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

    @Override
    protected Flux<Signal<Double, String>> getSensorSignal(DriverNetworkEvent event) {

        if (!(event instanceof OneWireNetworkTemperatureSample sample)) {
            return Flux.empty();
        }

        return Flux.just(new Signal<>(event.timestamp, sample.sample, sample.address));
    }

    @Override
    protected void handleArrival(DriverNetworkEvent event) {

        // VT: FIXME: Reimplement this as a subscriber

        if (!(event instanceof OneWireNetworkArrival arrivalEvent)) {
            return;
        }

        devicesPresent.add(arrivalEvent.address);
        address2path.put(arrivalEvent.address, arrivalEvent.path);
    }

    @Override
    protected void handleDeparture(DriverNetworkEvent event) {

        // VT: FIXME: Reimplement this as a subscriber
        if (!(event instanceof OneWireNetworkDeparture departureEvent)) {
            return;
        }

        devicesPresent.remove(departureEvent.address);
        address2path.remove(departureEvent.address);

        logger.error("Departure not handled completely: {}", ((OneWireNetworkDeparture) event).address );
    }

    @Override
    protected void connect(FluxSink<DriverNetworkEvent> sink) {

        if (this.monitor != null) {
            throw new IllegalStateException("Fatal programming error, can't connect() more than once");
        }

        logger.info("Starting 1-Wire monitor for {}", endpoint);
        this.monitor = new OneWireNetworkMonitor(endpoint, sink);
    }

    @Override
    protected SwitchProxy getSwitchProxy(String address, long heartbeatSeconds) {
        return new OneWireSwitchProxy(address, heartbeatSeconds);
    }

    @Override
    protected void checkSwitchAddress(String address) {
        new IntegerChannelAddress(address);
        // Syntax is OK if we made it this far, let's proceed
    }
    @Override
    protected DriverNetworkMonitor<DSPortAdapter> getMonitor() {
        return monitor;
    }

    @Override
    public void close() {
        logger.debug("close(): NOP");
    }

    public class OneWireSwitchProxy extends SwitchProxy {
        public OneWireSwitchProxy(String address, long heartbeatSeconds) {
            super(address, heartbeatSeconds);
        }

        @Override
        protected DriverCommand<DSPortAdapter> getSetSwitchCommand(String address, FluxSink<DriverCommand<DSPortAdapter>> commandSink, UUID messageId, boolean state) {
            var channelAddress = new IntegerChannelAddress(address);
            return new OneWireSetSwitchCommand(
                    messageId,
                    commandSink,
                    (OWPathResolver) getMonitor(),
                    address,
                    address2path.get(channelAddress.hardwareAddress),
                    state);
        }

        @Override
        protected Mono<Boolean> expectSwitchState(UUID messageId) {
            return getDriverFlux()
                    .filter(OneWireSwitchState.class::isInstance)
                    .map(OneWireSwitchState.class::cast)
                    .filter(s -> s.correlationId.equals(messageId))
                    .doOnNext(s -> logger.debug("{}", s))
                    .map(OneWireSwitchState::getState)
                    .next();
        }
    }
}
