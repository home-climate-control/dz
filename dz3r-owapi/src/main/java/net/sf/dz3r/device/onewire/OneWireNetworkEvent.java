package net.sf.dz3r.device.onewire;

import java.time.Instant;

/**
 * Base class for all events that can be emitted by a 1-Wire network.
 *
 * @param <P> Payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkEvent<P> {

    public final Instant timestamp;
    public final P payload;

    public OneWireNetworkEvent(Instant timestamp, P payload) {
        this.timestamp = timestamp;
        this.payload = payload;
    }
}
