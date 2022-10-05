package net.sf.dz3.device.sensor.impl.xbee;

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.XBeeAddress64;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.impl.AbstractDeviceContainer;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3.util.digest.MessageDigestCache;
import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

/**
 * XBee sensor container.
 *
 * Requires XBee to be programmed to broadcast sensor readings. Setting sample rate to one in 30 seconds provides
 * acceptable quality of control.
 *
 * @see <a href="https://www.digi.com/resources/documentation/digidocs/pdfs/90000976.pdf">Zigbee RF Modules XBEE2, XBEEPRO2, PRO S2B User Guide</a>,
 * I/O sample rate (IR command).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2020
 */
public class XBeeSensor extends AbstractDeviceContainer implements AnalogSensor {

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<>();

    private final XBeeDeviceContainer container;
    private final StringChannelAddress address;
    private final String sourceName;
    private final String signature;
    private final SensorType type;

    /**
     * Create an instance.
     *
     * @param container XBee device container to communicate through.
     * @param address Switch address.
     * @param type Sensor type.
     */
    public XBeeSensor(final XBeeDeviceContainer container, final String address, SensorType type) {

        this.container = container;
        this.address = new StringChannelAddress(address);
        this.type = type;

        this.sourceName = type + this.address.toString();
        this.signature = MessageDigestCache.getMD5(type + getAddress()).substring(0, 19);
    }

    @Override
    public String getAddress() {

        return address.toString();
    }

    @Override
    public DataSample<Double> getSignal() {

        ThreadContext.push("getSignal(" + address + ")");
        Marker m = new Marker("getSignal(" + address + ")");

        try {

            XBeeAddress64 xbeeAddress = Parser.parse(address.hardwareAddress);
            String channel = address.channel;

            RemoteAtRequest request = new RemoteAtRequest(xbeeAddress, "IS");
            AtCommandResponse rsp = (AtCommandResponse) container.sendSynchronous(request, XBeeConstants.TIMEOUT_IS_MILLIS);

            logger.debug("{} response: {}", channel, rsp);

            if (rsp.isError() || !rsp.isOk()) {

                throw new IOException(channel + " + query failed, response: " + rsp);
            }

            IoSample sample = new IoSample(rsp.getValue(), xbeeAddress, logger);

            logger.debug("sample: {}", sample);

            return new DataSample<>(System.currentTimeMillis(), sourceName, signature, sample.getChannel(channel), null);

        } catch (Throwable t) {

            throw new IllegalStateException("Unable to read " + address, t);

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {

        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {

        dataBroadcaster.removeConsumer(consumer);
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Analog Sensor",
                Integer.toHexString(hashCode()),
                "Reads XBee analog inputs");
    }

    /**
     * Broadcast a signal sample.
     *
     * @param timestamp Timestamp to mark the sample with.
     * @param value Signal value. Can be {@code null} (mutually exclusive with {@code t}).
     * @param t Signal exception. Can be {@code null} (mutually exclusive with {@code value}).
     */
    public void broadcast(long timestamp, Double value, Throwable t) {

        DataSample<Double> signal = new DataSample<>(timestamp, sourceName, signature, value, t);

        dataBroadcaster.broadcast(signal);
    }

    @Override
    public String getName() {

        return "XBee Analog Sensor";
    }

    @Override
    public SensorType getType() {

        return type;
    }
}
