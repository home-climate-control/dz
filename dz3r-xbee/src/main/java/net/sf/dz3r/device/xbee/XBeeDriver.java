package net.sf.dz3r.device.xbee;

import net.sf.dz3r.device.driver.AbstractDeviceDriver;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.xbee.event.XBeeNetworkArrival;
import net.sf.dz3r.device.xbee.event.XBeeNetworkIOSample;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Driver for XBee subsystem using the <a href="https://github.com/home-climate-control/xbee-api-reactive">xbee-api-reactive</a> library.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class XBeeDriver extends AbstractDeviceDriver<String, Double, String> {

    private final String port;

    private XBeeNetworkMonitor monitor;

    public XBeeDriver(String port) {
        this.port = port;
    }

    @Override
    protected Flux<Signal<Double, String>> getSensorSignal(DriverNetworkEvent event) {

        logger.info("getSensorSignal: {}", event);

        if (!(event instanceof XBeeNetworkIOSample)) {
            return Flux.empty();
        }

        var sample = ((XBeeNetworkIOSample) event).sample;

        return Flux.create(sink -> {
            // We're only interested in analog data for now, and there's 4 channels of it
            for (var pin = 0; pin < 4; pin++) {
                if (sample.isAnalogEnabled(pin)) {

                    String address = ((XBeeNetworkIOSample) event).address + ":A" + pin;

                    // This will take care of the device *and* channel address.
                    // There's no need to send an arrival notification.
                    devicesPresent.add(address);

                    sink.next(new Signal<>(event.timestamp, Double.valueOf(sample.getAnalog(pin)), address));
                }
            }
            sink.complete();
        });
    }

    @Override
    protected void handleArrival(DriverNetworkEvent event) {

        if (!(event instanceof XBeeNetworkArrival)) {
            return;
        }

        var arrivalEvent = (XBeeNetworkArrival) event;

        // This will only add the *device* address, but not individual channel address.
        devicesPresent.add(arrivalEvent.address);
    }

    @Override
    protected void handleDeparture(DriverNetworkEvent event) {
        // There will be no departure notification, Xbee devices just disappear.
        // This will have to be handled elsewhere.
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
}
