package net.sf.dz3r.device.xbee;

import com.homeclimatecontrol.xbee.XBeeReactive;
import net.sf.dz3r.common.StringChannelAddress;
import net.sf.dz3r.device.driver.AbstractDeviceDriver;
import net.sf.dz3r.device.driver.DriverNetworkMonitor;
import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.xbee.command.XBeeSetSwitchCommand;
import net.sf.dz3r.device.xbee.event.XBeeNetworkArrival;
import net.sf.dz3r.device.xbee.event.XBeeNetworkIOSample;
import net.sf.dz3r.device.xbee.event.XBeeSwitchState;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.filter.AnalogConverter;
import net.sf.dz3r.signal.filter.AnalogConverterLM34;
import net.sf.dz3r.signal.filter.AnalogConverterTMP36;
import net.sf.dz3r.signal.filter.ConvertingFilter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Driver for XBee subsystem using the <a href="https://github.com/home-climate-control/xbee-api-reactive">xbee-api-reactive</a> library.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class XBeeDriver extends AbstractDeviceDriver<String, Double, String, XBeeReactive> {

    private final String port;

    private XBeeNetworkMonitor monitor;

    public XBeeDriver(String port) {
        this.port = port;
    }

    @Override
    protected Flux<Signal<Double, String>> getSensorSignal(DriverNetworkEvent event) {

        logger.info("getSensorSignal: {}", event);

        if (!(event instanceof XBeeNetworkIOSample xbeeSample)) {
            return Flux.empty();
        }

        var sample = xbeeSample.sample;

        return Flux.create(sink -> {
            // We're only interested in analog data for now, and there's 4 channels of it (plus supply voltage)
            for (var kv : sample.analogSamples.entrySet()) {

                var pin = kv.getKey();
                var signal = Double.valueOf(kv.getValue());
                String address = ((XBeeNetworkIOSample) event).address + ":A" + pin;

                // This will take care of the device *and* channel address.
                // There's no need to send an arrival notification.
                devicesPresent.add(address);

                sink.next(new Signal<>(event.timestamp, signal, address));
            }
            sink.complete();
        });
    }

    @Override
    protected void handleArrival(DriverNetworkEvent event) {

        if (!(event instanceof XBeeNetworkArrival arrivalEvent)) {
            return;
        }

        // This will only add the *device* address, but not individual channel address.
        devicesPresent.add(arrivalEvent.address);
    }

    @Override
    protected void handleDeparture(DriverNetworkEvent event) {
        // There will be no departure notification, Xbee devices just disappear.
        // This will have to be handled elsewhere.
    }

    @Override
    protected SwitchProxy getSwitchProxy(String address, long heartbeatSeconds) {
        return new XBeeSwitchProxy(address, heartbeatSeconds);
    }

    @Override
    protected void checkSwitchAddress(String address) {
        new StringChannelAddress(address);
        // Syntax is OK if we made it this far, let's proceed
    }

    @Override
    protected DriverNetworkMonitor<XBeeReactive> getMonitor() {
        return monitor;
    }

    @Override
    protected void connect(FluxSink<DriverNetworkEvent> sink) {

        if (this.monitor != null) {
            throw new IllegalStateException("Fatal programming error, can't connect() more than once");
        }

        logger.info("Starting XBee monitor for {}", port);
        this.monitor = new XBeeNetworkMonitor(port, sink);
    }

    @Override
    public void close() {
        monitor.close();
    }

    /**
     * Get the flux of readings with {@link AbstractDeviceDriver#getFlux(Comparable)}, and convert each value.
     *
     * @param address Device address to get the flux of readings for.
     * @param conversion Either {@code LM34}, or {@code TMP36}.
     *
     * @return Flux of device readings.
     *
     * @throws IllegalArgumentException if the {@code conversion} argument is neither of two supported.
     */
    public final Flux<Signal<Double, String>> getFlux(String address, String conversion) {
        AnalogConverter converter = switch (conversion.toUpperCase()) {
            case "LM34" -> new AnalogConverterLM34();
            case "TMP36" -> new AnalogConverterTMP36();
            default -> throw new IllegalArgumentException("Supported values are: LM34, TMP36");
        };

        return new ConvertingFilter<String>(converter).compute(getFlux(address));
    }

    private class XBeeSwitchProxy extends SwitchProxy {
        public XBeeSwitchProxy(String address, long heartbeatSeconds) {
            super(address, heartbeatSeconds);
        }

        @Override
        protected DriverCommand<XBeeReactive> getSetSwitchCommand(String address, FluxSink<DriverCommand<XBeeReactive>> commandSink, UUID messageId, boolean state) {
            return new XBeeSetSwitchCommand(
                    messageId,
                    commandSink,
                    address,
                    state);
        }

        @Override
        protected Mono<Boolean> expectSwitchState(UUID messageId) {
            return Mono.create(sink -> {
                try {

                    var state = getDriverFlux()
                            .filter(XBeeSwitchState.class::isInstance)
                            .map(XBeeSwitchState.class::cast)
                            .doOnNext(s -> logger.debug("{}", s))
                            .blockFirst()
                            .state;

                    sink.success(state);

                } catch (Exception e) {
                    sink.error(e);
                }
            });
        }
    }
}
