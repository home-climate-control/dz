package net.sf.dz3r.device.xbee.event;

import com.homeclimatecontrol.xbee.response.frame.IOSampleIndicator;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;

import java.time.Instant;

/**
 * IO sample obtained from a {@link ZNetRxIoSampleResponse}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class XBeeNetworkIOSample extends XBeeNetworkDeviceStateEvent {

    public final IOSampleIndicator sample;

    public XBeeNetworkIOSample(Instant timestamp, String address, IOSampleIndicator sample) {
        super(timestamp, null, address);
        this.sample = sample;
    }

    @Override
    public String toString() {
        return "{XBeeNetworkIOSample timestamp=" + timestamp
                + ", address=" + address
                + ", sample=" + sample
                + "}";
    }
}
