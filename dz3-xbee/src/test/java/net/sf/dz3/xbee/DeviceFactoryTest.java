package net.sf.dz3.xbee;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Ignore;

import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;

import junit.framework.TestCase;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.device.sensor.impl.xbee.Converter;
import net.sf.dz3.device.sensor.impl.xbee.IoSample;
import net.sf.dz3.device.sensor.impl.xbee.XBeeDeviceFactory;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

@Ignore
public class DeviceFactoryTest extends TestCase implements DataSink<Double> {

    private static final String serialPort = "/dev/ttyUSB0";

    private final Logger logger = LogManager.getLogger(getClass());
    private final Random rg = new Random();

    public void testSensor() throws XBeeException {

        ThreadContext.push("testSensor");

        XBee coordinator = new XBee();

        try {

            coordinator.open(serialPort, 9600);

            XBeeAddress64 addr64 = new XBeeAddress64("00 13 A2 00 40 62 AC 98");

            {
                RemoteAtRequest request = new RemoteAtRequest(addr64, "D0", new int[] {2});
                XBeeResponse rsp = coordinator.sendSynchronous(request, 5000);

                logger.info("D0 response: " + rsp);
            }
            {
                RemoteAtRequest request = new RemoteAtRequest(addr64, "D1", new int[] {2});
                XBeeResponse rsp = coordinator.sendSynchronous(request, 5000);

                logger.info("D1 response: " + rsp);
            }
            {
                RemoteAtRequest request = new RemoteAtRequest(addr64, "D2", new int[] {2});
                XBeeResponse rsp = coordinator.sendSynchronous(request, 5000);

                logger.info("D2 response: " + rsp);
            }
            {
                RemoteAtRequest request = new RemoteAtRequest(addr64, "D3", new int[] {2});
                XBeeResponse rsp = coordinator.sendSynchronous(request, 5000);

                logger.info("D3 response: " + rsp);
            }

            {
                RemoteAtRequest request = new RemoteAtRequest(addr64, "IS");
                XBeeResponse rsp = coordinator.sendSynchronous(request, 5000);

                logger.info("IS response: " + rsp);

                if (!rsp.isError()) {

                    int[] data = ((AtCommandResponse) rsp).getValue();

                    assertEquals("Unexpected buffer length", 12, data.length);

                    for (int offset = 4; offset < 12; offset += 2) {

                        int sensorReading = data[offset] << 8 | data[offset+1];

                        logger.info("@" + offset + ": " + Integer.toHexString(sensorReading) + " (" + Converter.ADC2C_TMP36(sensorReading) + ")");
                    }

                    for (int offset = 4; offset < 12; offset += 2) {

                        int sensorReading = data[offset] << 8 | data[offset+1];

                        logger.info("@" + offset + ": " + Integer.toHexString(sensorReading) + " (" + Converter.raw2mV(sensorReading) + ")");
                    }

                    IoSample sample = new IoSample(data, addr64, logger);

                    logger.info("IoSample: " + sample);
                }
            }

            {
                RemoteAtRequest request = new RemoteAtRequest(addr64, "RP", new int[] {0x05});
                XBeeResponse rsp = coordinator.sendSynchronous(request, 5000);

                logger.info("IR response: " + rsp);
            }
            {
                RemoteAtRequest request = new RemoteAtRequest(addr64, "%V", new int[] {2});
                XBeeResponse rsp = coordinator.sendSynchronous(request, 5000);

                logger.info("%V response: " + rsp);

                int[] data = ((AtCommandResponse) rsp).getValue();

                assertEquals("Unexpected buffer length", 2, data.length);

                int sensorReading = data[0] << 8 | data[1];

                logger.info("raw " + Integer.toHexString(sensorReading) + " (" + Converter.raw2mV(sensorReading) + "mV)");
            }
            {
                RemoteAtRequest request = new RemoteAtRequest(addr64, "IR", new int[] {0x14, 0x00});
//                RemoteAtRequest request = new RemoteAtRequest(addr64, "IR", new int[] {0});
                XBeeResponse rsp = coordinator.sendSynchronous(request, 5000);

                logger.info("IR response: " + rsp);
            }

            coordinator.close();

        } catch (Throwable t) {

            logger.error("Oops", t);

            try {

                coordinator.close();

            } catch (Throwable t2) {

                logger.error("Double oops", t2);
            }

            fail("Unexpected exception");

        } finally {
            ThreadContext.pop();
        }
    }

    public void testStartStop() throws InterruptedException {

        ThreadContext.push("testStartStop");

        try {

            XBeeDeviceFactory deviceFactory = new XBeeDeviceFactory(serialPort);
            String relayAddress = "0013A200.405D8027";
            String sensorAddress = "0013A200.4062AC98";

            if (!deviceFactory.start().waitFor()) {
                fail("Unable to start the device factory");
            }

            // This may blow up - by default some of the pins are not configured
            // as digital outputs

            //read(deviceFactory, deviceAddress);

            List<Boolean> step2 = writeRandom(deviceFactory, relayAddress);
            List<Boolean> step3 = read(deviceFactory, relayAddress);

            for (int offset = 0; offset < step2.size(); offset++) {

                logger.info("@" + offset + ": " + step2.get(offset));

                assertEquals("Mismatch @" + offset, step2.get(offset), step3.get(offset));
            }

            readSensor(deviceFactory, sensorAddress);
//            readSensor(deviceFactory, sensorAddress);
//            readSensor(deviceFactory, sensorAddress);

            // Wait for more samples

            long sleep = 20000;
            logger.info("Sleeping for " + sleep + "ms");
            Thread.sleep(sleep);

            deviceFactory.stop().waitFor();

        } catch (Throwable t) {

            logger.error("Oops", t);
            fail("Unexpected exception");

        } finally {
            ThreadContext.pop();
        }
    }

    private void readSensor(XBeeDeviceFactory deviceFactory, String sensorAddress) {

        for (int offset = 0; offset < 4; offset++) {

            String channelAddress = sensorAddress + ":A" + offset;

            AnalogSensor sensor = deviceFactory.getTemperatureSensor(channelAddress);

            sensor.addConsumer(this);

            logger.info("sensor: " + sensor);
            logger.info("signal: " + sensor.getSignal());

            // VT: FIXME: The rest of the sensor test here
        }
    }

    private List<Boolean> read(XBeeDeviceFactory deviceFactory, String deviceAddress) throws IOException {

        List<Boolean> read = new LinkedList<Boolean>();

        for (int offset = 0; offset < 4; offset++) {

            String address = deviceAddress + ":D" + offset;

            logger.info("reading " + address);
            Switch s = deviceFactory.getSwitch(address);

            boolean state = s.getState();

            logger.info("Switch state: " + state);
            read.add(state);
        }

        return read;
    }

    private List<Boolean> writeRandom(XBeeDeviceFactory deviceFactory, String deviceAddress) throws IOException {

        List<Boolean> written = new LinkedList<Boolean>();

        for (int offset = 0; offset < 4; offset++) {

            String address = deviceAddress + ":D" + offset;

            logger.info("reading " + address);
            Switch s = deviceFactory.getSwitch(address);

            boolean state = rg.nextBoolean();

            s.setState(state);

            logger.info("Switch state: " + state);
            written.add(state);
        }

        return written;
    }

    @Override
    public void consume(DataSample<Double> signal) {

        logger.info("consume: " + signal);
    }
}
