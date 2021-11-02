package net.sf.dz3.xbee;

import com.homeclimatecontrol.xbee.AddressParser;
import com.rapplogic.xbee.api.AtCommand;
import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeePacket;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.util.ByteUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.rapplogic.xbee.api.AtCommand.Command.AI;
import static com.rapplogic.xbee.api.AtCommand.Command.AP;
import static com.rapplogic.xbee.api.AtCommand.Command.BD;
import static com.rapplogic.xbee.api.AtCommand.Command.CH;
import static com.rapplogic.xbee.api.AtCommand.Command.D0;
import static com.rapplogic.xbee.api.AtCommand.Command.DD;
import static com.rapplogic.xbee.api.AtCommand.Command.EE;
import static com.rapplogic.xbee.api.AtCommand.Command.HV;
import static com.rapplogic.xbee.api.AtCommand.Command.ID;
import static com.rapplogic.xbee.api.AtCommand.Command.MY;
import static com.rapplogic.xbee.api.AtCommand.Command.NC;
import static com.rapplogic.xbee.api.AtCommand.Command.ND;
import static com.rapplogic.xbee.api.AtCommand.Command.NI;
import static com.rapplogic.xbee.api.AtCommand.Command.NJ;
import static com.rapplogic.xbee.api.AtCommand.Command.NO;
import static com.rapplogic.xbee.api.AtCommand.Command.NP;
import static com.rapplogic.xbee.api.AtCommand.Command.NT;
import static com.rapplogic.xbee.api.AtCommand.Command.OI;
import static com.rapplogic.xbee.api.AtCommand.Command.OP;
import static com.rapplogic.xbee.api.AtCommand.Command.P0;
import static com.rapplogic.xbee.api.AtCommand.Command.SD;
import static com.rapplogic.xbee.api.AtCommand.Command.VR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class XbeeApiTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    void packetEscape() {

        final int[] knownGoodPacket = new int[] {
                0x7E, // Start delimiter
                0x00, // Length MSB
                0x0F, // Length LSB
                0x17, // 'Remote AT' command (frame data start)
                0x01, // Frame ID
                0x00,
                0x7D, // 0x7D33 is 0x13 escaped
                0x33,
                0xA2,
                0x00,
                0x40,
                0x62,
                0xAC,
                0x98,
                0xFF,
                0xFE,
                0x02, // 0x02 means 'apply changes'
                0x44,
                0x30,
                0xD9
        };

        ThreadContext.push("testPacketEscape");

        try {

            XBeeAddress64 xbeeAddress = AddressParser.parse("0013A200.4062AC98");

            RemoteAtRequest request = new RemoteAtRequest(xbeeAddress, D0);

            request.setApplyChanges(true);

            XBeePacket packet = request.getXBeePacket();

            int[] byteBuffer = packet.getByteArray();

            logger.info("Source: " + ByteUtils.toBase16(knownGoodPacket));
            logger.info("Packet: " + ByteUtils.toBase16(byteBuffer));

            assertThat(byteBuffer.length).isEqualTo(knownGoodPacket.length);

            for (int offset = 0; offset < knownGoodPacket.length; offset++) {

                assertThat(byteBuffer[offset]).as("Packet content mismatch @" + offset).isEqualTo(knownGoodPacket[offset]);
            }

        } catch (Throwable t) {

            logger.error("Oops", t);
            fail(t.getMessage());

        } finally {

            ThreadContext.pop();
        }
    }

    @SuppressWarnings("squid:S1607")
    @Disabled("Enable this if you have the actual hardware (you will need to adjust the addresses, too")
    @Test
    public void testXbee() throws XBeeException {

        // VT: NOTE: squid:S1607 - Actual hardware is necessary for this test, so disabled

        ThreadContext.push("testXBee");

        try {

            XBee xbee = new XBee();

            try {
                xbee.open("/dev/ttyUSB0", 9600);

                // Find out who's around

                AT(xbee, MY);
                AT(xbee, NC);
                AT(xbee, NI);
                AT(xbee, NP);
                AT(xbee, DD);
                AT(xbee, CH);
                AT(xbee, ID);
                AT(xbee, OP);
                AT(xbee, OI);
                AT(xbee, NT);
                AT(xbee, NO);
                AT(xbee, SD);
                AT(xbee, NJ);
                AT(xbee, EE);
                AT(xbee, AP);
                AT(xbee, BD);
                AT(xbee, P0);
                AT(xbee, VR);
                AT(xbee, HV);
                AT(xbee, AI);

                AT(xbee, ND);
                AT(xbee, AI);

                AT(xbee, AP, 2);

                for (int offset = 0; offset < 4; offset++) {

                    String target = "D" + offset;
                    XBeeAddress64 addr64 = new XBeeAddress64(0x00, 0x13, 0xa2, 0x00, 0x40, 0x5d, 0x80, 0x27);
//                  XBeeAddress64 addr64 = new XBeeAddress64("00 13 A2 00 40 5D 80 27");
//                  XBeeAddress16 addr16 = new XBeeAddress16(0x48, 0xFE);
//                  XBeeAddress64 addr64 = new XBeeAddress64(0x00, 0x13, 0xa2, 0x00, 0x40, 0x62, 0xac, 0x98);

                    ThreadContext.push("405D8027:" + offset + " write 5");


                    try {

                        logger.info("creating request to " + addr64);

                        // Send the request to turn on D${offset}
                        RemoteAtRequest request = new RemoteAtRequest(addr64, AtCommand.Command.valueOf(target), new int[] {5});
//                        ZNetRemoteAtRequest request = new ZNetRemoteAtRequest(XBeeRequest.DEFAULT_FRAME_ID, addr64, addr16, true, target, new int[] {5});
                        XBeeResponse rsp = xbee.sendSynchronous(request, Duration.ofSeconds(5));

                        logger.info(target + " response: " + rsp);

                        AtCommandResponse response = (AtCommandResponse) xbee.getResponse();

                        if (response.isOk()) {
                            logger.info("Successfully turned " + target);
                        } else {
                            logger.error("Attempt to turn on " + target + " failed.  Status: " + response.getStatus());
                        }

                    } finally {
                        ThreadContext.pop();
                    }

                    ThreadContext.push("405D8027:" + offset + " read");

                    try {

                        // Query D${offset} status
                        RemoteAtRequest request = new RemoteAtRequest(addr64, AtCommand.Command.valueOf(target));

                        XBeeResponse rsp = xbee.sendSynchronous(request, Duration.ofSeconds(10));

                        logger.info(target + " response: " + rsp);

                        AtCommandResponse response = (AtCommandResponse) xbee.getResponse();

                        if (response.isOk()) {
                            logger.info("Successfully turned " + target);
                        } else {
                            logger.error("Attempt to turn on " + target + " failed.  Status: " + response.getStatus());
                        }

                    } finally {
                        ThreadContext.pop();
                    }

                    ThreadContext.push("405D8027:" + offset + " write 4");

                    try {

                        logger.info("creating request to " + addr64);

                        // Send the request to turn on D${offset}
                        RemoteAtRequest request = new RemoteAtRequest(addr64, AtCommand.Command.valueOf(target), new int[] {4});
//                        ZNetRemoteAtRequest request = new ZNetRemoteAtRequest(XBeeRequest.DEFAULT_FRAME_ID, addr64, addr16, true, target, new int[] {5});
                        XBeeResponse rsp = xbee.sendSynchronous(request, Duration.ofSeconds(5));

                        logger.info(target + " response: " + rsp);

                        AtCommandResponse response = (AtCommandResponse) xbee.getResponse();

                        if (response.isOk()) {
                            logger.info("Successfully turned " + target);
                        } else {
                            logger.error("Attempt to turn on " + target + " failed.  Status: " + response.getStatus());
                        }

                    } finally {
                        ThreadContext.pop();
                    }
                }

            } catch (Throwable t) {
                logger.fatal("Oops", t);
            } finally {

                // shutdown the serial port and associated threads
                xbee.close();
            }

            // Just pass.
            assertThat(true).isTrue();

        } finally {
            ThreadContext.pop();
        }
    }

    private void AT(XBee xbee, AtCommand.Command command) {

        ThreadContext.push("AT");

        try {

            XBeeResponse rsp = xbee.sendSynchronous(new AtCommand(command), Duration.ofSeconds(10));

            logger.info(command + " response: " + rsp);
        } catch (Throwable t) {
            logger.fatal(command + " failed", t);
        } finally {
            ThreadContext.pop();
        }
    }

    private void AT(XBee xbee, AtCommand.Command command, int value) {

        ThreadContext.push("AT");

        try {

            XBeeResponse rsp = xbee.sendSynchronous(new AtCommand(command, value), Duration.ofSeconds(10));

            logger.info(command + " response: " + rsp);
        } catch (Throwable t) {
            logger.fatal(command + " failed", t);
        } finally {
            ThreadContext.pop();
        }
    }
}
