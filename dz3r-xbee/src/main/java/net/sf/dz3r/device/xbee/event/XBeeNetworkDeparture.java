package net.sf.dz3r.device.xbee.event;

import java.time.Instant;

/**
 * XBee network device departure.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class XBeeNetworkDeparture extends XBeeNetworkTopologyEvent {

    public XBeeNetworkDeparture(Instant timestamp, String address) {
        super(timestamp, null, address);
    }

    @Override
    public String toString() {
        return "{XBeeNetworkDeparture timestamp=" + timestamp
                + ", address=" + address + "}";
    }
}
