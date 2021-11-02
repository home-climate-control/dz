package net.sf.dz3r.device.xbee;

import net.sf.dz3r.device.driver.AbstractDeviceDriver;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
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
        logger.warn("Not implemented, dropped on the floor: {}", event);
        return Flux.empty();
    }

    @Override
    protected void handleArrival(DriverNetworkEvent event) {

    }

    @Override
    protected void handleDeparture(DriverNetworkEvent event) {

    }

    @Override
    protected void connect(FluxSink<DriverNetworkEvent> sink) {

        if (this.monitor != null) {
            throw new IllegalStateException("Fatal programming error, can't connect() more than once");
        }

        logger.info("Starting XBee monitor for {}", port);
        this.monitor = new XBeeNetworkMonitor(port, sink);
    }
}
