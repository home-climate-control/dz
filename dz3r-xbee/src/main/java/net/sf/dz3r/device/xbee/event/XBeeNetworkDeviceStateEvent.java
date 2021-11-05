package net.sf.dz3r.device.xbee.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all device status related events on an XBee network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class XBeeNetworkDeviceStateEvent extends XBeeNetworkEvent {

    public final String address;

    public XBeeNetworkDeviceStateEvent(Instant timestamp, UUID correlationId, String address) {
        super(timestamp, correlationId);
        this.address = address;
    }
}
