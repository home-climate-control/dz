package net.sf.dz3r.device.onewire.event;

import com.dalsemi.onewire.utils.OWPath;

import java.time.Instant;

/**
 * 1-Wire network device arrival.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkArrival extends OneWireNetworkTopologyEvent {

    public final OWPath path;

    public OneWireNetworkArrival(Instant timestamp, String address, OWPath path) {
        super(timestamp, null, address);
        this.path = path;
    }

    @Override
    public String toString() {
        return "{1-Wire network arrival timestamp=" + timestamp
                + ", address=" + address
                + ", path=" + path + "}";
    }
}
