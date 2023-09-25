package net.sf.dz3r.device.xbee.event;

import java.time.Instant;

/**
 * XBee network device arrival.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class XBeeNetworkArrival extends XBeeNetworkTopologyEvent {

    public XBeeNetworkArrival(Instant timestamp, String address) {
        super(timestamp, null, address);
    }

    @Override
    public String toString() {
        return "{XBeeNetworkArrival timestamp=" + timestamp
                + ", address=" + address + "}";
    }
}
