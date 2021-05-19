package net.sf.dz3.xbee;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.dz3.device.sensor.impl.xbee.Parser;

public class AddressParserTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    public void Parser() {

        Parser.parse("48 fe 00 13 a2 00 40 5d");
        Parser.parse("48fe0013 a200405d");
        Parser.parse("48fe0013a200405d");
        Parser.parse("00000013a200405d");

        // It might be nice to be able to parse something like this someday, but of minimal priority
        // XBeeAddress64 addr3 = Parser.parse("0x48 0xfe 0x00 0x13 0xa2 0x00 0x40 0x5d");

        StringChannelAddress s1 = new StringChannelAddress("48 fe 00 13 a2 00 40 5d:0");
        StringChannelAddress s2 = new StringChannelAddress("48fe0013 a200405d:1");
        StringChannelAddress s3 = new StringChannelAddress("48fe0013a200405d:2");
        StringChannelAddress s4 = new StringChannelAddress("00000013a200405d:2");

        logger.info("Address 1: " + s1);
        logger.info("Address 2: " + s2);
        logger.info("Address 3: " + s3);
        logger.info("Address 4: " + s4);

        assertEquals("48 fe 00 13 a2 00 40 5d:0", s1.toString());
        assertEquals("48fe0013 a200405d:1", s2.toString());
        assertEquals("48fe0013a200405d:2", s3.toString());
        assertEquals("00000013a200405d:2", s4.toString());
    }

    @Test
    public void highBit() {

        assertEquals("0013A200.405D8027", Parser.render4x4("00 13 a2 00 40 5d 80 27"));
        assertEquals("0013A200.F05D8027", Parser.render4x4("00 13 a2 00 f0 5d 80 27"));
    }
}
