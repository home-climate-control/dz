package net.sf.dz3r.device.onewire.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all topology related events on a 1-Wire network.
 *
 * These include, but are not limited to, {@link OneWireNetworkArrival arrivals},
 * {@link OneWireNetworkDeparture departures}, and path discoveries.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkTopologyEvent extends OneWireNetworkEvent {

    public final String address;

    public OneWireNetworkTopologyEvent(Instant timestamp, UUID correlationId, String address) {
        super(timestamp, correlationId);
        this.address = address;
    }
}
