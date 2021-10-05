package net.sf.dz3r.device.onewire.event;

import java.time.Instant;

/**
 * Temperature sample obtained from a {@link com.dalsemi.onewire.container.TemperatureContainer}.
 *
 * This event is guaranteed not to contain values of 85°C which indicate a 1-Wire network error.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 *
 * @see net.sf.dz3r.device.onewire.command.OneWireCommandReadTemperatureAll
 */
public class OneWireNetworkTemperatureSample extends OneWireNetworkDeviceStatusEvent {

    public final double sample;

    public OneWireNetworkTemperatureSample(Instant timestamp, String address, double sample) {
        super(timestamp, null, address);
        this.sample = sample;
    }

    @Override
    public String toString() {
        return "{1-Wire temperature sample timestamp=" + timestamp
                + ", address=" + address
                + ", sample=" + sample
                + "°C}";
    }
}
