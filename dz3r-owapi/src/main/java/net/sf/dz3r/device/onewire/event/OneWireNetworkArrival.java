package net.sf.dz3r.device.onewire.event;

import java.time.Instant;

/**
 * 1-Wire network device arrival.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkArrival extends OneWireNetworkTopologyEvent {

    public OneWireNetworkArrival(Instant timestamp, String address) {
        super(timestamp, address);
    }

    @Override
    public String toString() {
        return "{1-Wire network arrival timestamp=" + timestamp
                + ", address=" + address + "}";
    }
}
