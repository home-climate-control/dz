package net.sf.dz3r.device.xbee.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all topology related events on a XBee network.
 *
 * These include, but are not limited to, {@link XBeeNetworkArrival arrivals},
 * {@link XBeeNetworkDeparture departures}, and path discoveries.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class XBeeNetworkTopologyEvent extends XBeeNetworkEvent {

    public final String address;

    public XBeeNetworkTopologyEvent(Instant timestamp, UUID correlationId, String address) {
        super(timestamp, correlationId);
        this.address = address;
    }
}
