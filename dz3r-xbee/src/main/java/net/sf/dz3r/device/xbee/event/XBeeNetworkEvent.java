package net.sf.dz3r.device.xbee.event;

import java.time.Instant;

/**
 * Base class for all events that can be emitted by an XBee network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class XBeeNetworkEvent {

    public final Instant timestamp;

    public XBeeNetworkEvent(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
