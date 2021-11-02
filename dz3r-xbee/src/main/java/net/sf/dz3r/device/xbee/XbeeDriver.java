package net.sf.dz3r.device.xbee;

import net.sf.dz3r.device.AbstractDeviceDriver;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

/**
 * Driver for XBee subsystem using the <a href="https://github.com/home-climate-control/xbee-api-reactive">xbee-api-reactive</a> library.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class XbeeDriver extends AbstractDeviceDriver<String, Double, String> {

    @Override
    protected Flux<Signal<Double, String>> getSensorsFlux() {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
