package net.sf.dz3r.device.onewire.event;

import java.time.Instant;
import java.util.UUID;

public class OneWireSwitchState extends OneWireNetworkDeviceStateEvent {

    public final boolean state;

    public OneWireSwitchState(Instant timestamp, UUID correlationId, String address, boolean state) {
        super(timestamp, correlationId, address);
        this.state = state;
    }

    @Override
    public String toString() {
        return "{OneWireSwitchState timestamp=" + timestamp
                + ", address=" + address
                + ", state=" + state
                + "}";
    }
}
