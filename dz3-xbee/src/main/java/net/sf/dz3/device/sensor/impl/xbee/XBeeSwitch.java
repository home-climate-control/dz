package net.sf.dz3.device.sensor.impl.xbee;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.RemoteAtRequest;
import net.sf.dz3.device.sensor.Addressable;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.dz3.instrumentation.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;

/**
 * XBee switch container.
 *
 * Currently, this container is hardcoded to support the relay shield
 * at http://www.seeedstudio.com/depot/relay-shield-p-641.html,
 * but support will be soon extended to all XBee pins that can be configured as
 * digital outputs.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public class XBeeSwitch implements Switch {

    private final Logger logger = LogManager.getLogger(getClass());
    protected final DataBroadcaster<Boolean> dataBroadcaster = new DataBroadcaster<>();

    private final XBeeDeviceContainer container;
    private final StringChannelAddress address;

    /**
     * Create an instance.
     *
     * @param container XBee device container to communicate through.
     * @param address Switch address.
     */
    public XBeeSwitch(final XBeeDeviceContainer container, final String address) {

        this.container = container;
        this.address = new StringChannelAddress(address);
    }

    @Override
    public boolean getState() throws IOException {

        ThreadContext.push("read(" + address + ")");
        var m = new Marker("read(" + address + ")");

        try {

            var xbeeAddress = Parser.parse(address.hardwareAddress);
            var channel = address.channel;

            var request = new RemoteAtRequest(xbeeAddress, channel);
            var rsp = (AtCommandResponse) container.sendSynchronous(request, XBeeConstants.TIMEOUT_AT_MILLIS);

            logger.info("{} response: {}", channel, rsp);

            if (rsp.isError() || !rsp.isOk()) {

                throw new IOException(channel + " + query failed, response: " + rsp);
            }

            int[] buffer = rsp.getValue();

            if (buffer.length != 1) {

                throw new IOException("Unexpected buffer size " + buffer.length);
            }

            switch (buffer[0]) {
            case 4:

                return false;

            case 5:

                return true;

            default:

                throw new IOException(channel + " is not configured as switch, state is " + buffer[0]);
            }

        } catch (Throwable t) { // NOSONAR Consequences have been considered

            throw new IOException("Unable to read " + address, t);

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    @Override
    public void setState(boolean state) throws IOException {

        ThreadContext.push("write(" + address + ")");
        var m = new Marker("write(" + address + ")");

        try {

            var xbeeAddress = Parser.parse(address.hardwareAddress);
            var channel = address.channel;

            int deviceState = state ? 5 : 4;
            var request = new RemoteAtRequest(xbeeAddress, channel, new int[] {deviceState});
            var rsp = (AtCommandResponse) container.sendSynchronous(request, XBeeConstants.TIMEOUT_AT_MILLIS);

            logger.info("{} response: {}", channel, rsp);
            dataBroadcaster.broadcast(new DataSample<>(System.currentTimeMillis(), getAddress(), getAddress(), state, null));

            if (rsp.isError() || !rsp.isOk()) {

                throw new IOException(channel + " + query failed, response: " + rsp);
            }

        } catch (Throwable t) { // NOSONAR Consequences have been considered

            throw new IOException("Unable to write " + address, t);

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    @Override
    public String getAddress() {
        return address.toString();
    }

    @Override
    public final void addConsumer(DataSink<Boolean> consumer) {
        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public final void removeConsumer(DataSink<Boolean> consumer) {
        dataBroadcaster.removeConsumer(consumer);
    }


    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                Integer.toHexString(hashCode()) + "#" + getAddress().replace(":", "@"),
                "XBee switch");
    }

    @Override
    public int compareTo(Addressable o) {
        // Can't afford to collide with the wrapper
        return (getClass().getName() + getAddress()).compareTo((o.getClass().getName() + o.getAddress()));
    }
}
