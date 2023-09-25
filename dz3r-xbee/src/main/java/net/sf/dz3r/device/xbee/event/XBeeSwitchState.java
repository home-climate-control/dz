package net.sf.dz3r.device.xbee.event;

import java.time.Instant;
import java.util.UUID;

public class XBeeSwitchState extends XBeeNetworkDeviceStateEvent {

    public final boolean state;

    public XBeeSwitchState(Instant timestamp, UUID correlationId, String address, boolean state) {
        super(timestamp, correlationId, address);
        this.state = state;
    }

    @Override
    public String toString() {
        return "{XBeeSwitchState timestamp=" + timestamp
                + ", correlationId=" + correlationId
                + ", address=" + address
                + ", state=" + state
                + "}";
    }
}
