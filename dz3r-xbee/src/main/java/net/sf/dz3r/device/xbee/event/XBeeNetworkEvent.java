package net.sf.dz3r.device.xbee.event;

import net.sf.dz3r.device.driver.event.DriverNetworkEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events that can be emitted by an XBee network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class XBeeNetworkEvent extends DriverNetworkEvent {

    public XBeeNetworkEvent(Instant timestamp, UUID correlationId) {
        super(timestamp, correlationId);
    }
}
