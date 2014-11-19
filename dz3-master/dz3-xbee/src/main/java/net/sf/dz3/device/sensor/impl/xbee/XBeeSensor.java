package net.sf.dz3.device.sensor.impl.xbee;

import java.io.IOException;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.SensorType;
import net.sf.dz3.device.sensor.impl.AbstractDeviceContainer;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.dz3.instrumentation.Marker;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.jukebox.util.MessageDigestFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * XBee sensor container.
 * 
 * Requires XBee to be programmed to broadcast sensor readings. Setting sample rate to one in 30 seconds provides
 * acceptable quality of control.
 * 
 * @see ftp://ftp1.digi.com/support/documentation/90000976_G.pdf, I/O sample rate (IR command).
 *   
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2014
 */
public class XBeeSensor extends AbstractDeviceContainer implements AnalogSensor {
    
    private final Logger logger = Logger.getLogger(getClass());

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<Double>();
    
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
        this.signature = new MessageDigestFactory().getMD5(type + getAddress()).substring(0, 19);
    }

    @Override
    public String getAddress() {

        return address.toString();
    }

    @Override
    public DataSample<Double> getSignal() {
        
        NDC.push("getSignal(" + address + ")");
        Marker m = new Marker("getSignal(" + address + ")");

        try {
            
            XBeeAddress64 xbeeAddress = Parser.parse(address.hardwareAddress);
            String channel = address.channel;
            
            RemoteAtRequest request = new RemoteAtRequest(xbeeAddress, "IS");
            AtCommandResponse rsp = (AtCommandResponse) container.sendSynchronous(request, XBeeConstants.TIMEOUT_IS_MILLIS);

            logger.debug(channel + " response: " + rsp);

            if (rsp.isError()) {
                
                throw new IOException(channel + " + query failed, status: " + rsp.getStatus());
            }
            
            IoSample sample = new IoSample(rsp.getValue(), xbeeAddress, logger);
            
            logger.debug("sample: " + sample);
            
            return new DataSample<Double>(System.currentTimeMillis(), sourceName, signature, sample.getChannel(channel), null);
            
        } catch (Throwable t) {

            IOException secondary = new IOException("Unable to read " + address);

            secondary.initCause(t);

            throw new IllegalStateException("Not Implemented", t);

        } finally {

            m.close();
            NDC.pop();
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
        
        DataSample<Double> signal = new DataSample<Double>(timestamp, sourceName, signature, value, t);
        
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
