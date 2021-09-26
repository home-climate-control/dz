package net.sf.dz3r.device.onewire.event;

import java.time.Instant;

/**
 * Base class for all events that can be emitted by a 1-Wire network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkEvent {

    public final Instant timestamp;

    public OneWireNetworkEvent(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
