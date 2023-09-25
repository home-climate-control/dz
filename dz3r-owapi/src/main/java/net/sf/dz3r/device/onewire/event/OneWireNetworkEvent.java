package net.sf.dz3r.device.onewire.event;

import net.sf.dz3r.device.driver.event.DriverNetworkEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events that can be emitted by a 1-Wire network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkEvent extends DriverNetworkEvent {

    public OneWireNetworkEvent(Instant timestamp, UUID correlationId) {
        super(timestamp, correlationId);
    }
}
