package net.sf.dz3r.device.onewire.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all device status related events on a 1-Wire network.
 *
 * These include, but are not limited to, {@link OneWireNetworkTemperatureSample}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkDeviceStatusEvent extends OneWireNetworkEvent {

    public final String address;

    public OneWireNetworkDeviceStatusEvent(Instant timestamp, UUID correlationId, String address) {
        super(timestamp, correlationId);
        this.address = address;
    }
}
