package net.sf.dz3r.device.onewire.event;

import java.time.Instant;

/**
 * 1-Wire network device departure.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkDeparture extends OneWireNetworkTopologyEvent {

    public OneWireNetworkDeparture(Instant timestamp, String address) {
        super(timestamp, address);
    }

    @Override
    public String toString() {
        return "{1-Wire network departure timestamp=" + timestamp
                + ", address=" + address + "}";
    }
}
