package net.sf.dz3.xbee;

import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import net.sf.dz3.device.sensor.impl.xbee.Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressParserTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    public void parser() {

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

        assertThat(s1.toString()).isEqualTo("48 fe 00 13 a2 00 40 5d:0");
        assertThat(s2.toString()).isEqualTo("48fe0013 a200405d:1");
        assertThat(s3.toString()).isEqualTo("48fe0013a200405d:2");
        assertThat(s4.toString()).isEqualTo("00000013a200405d:2");
    }

    @Test
    public void highBit() {

        assertThat(Parser.render4x4("00 13 a2 00 40 5d 80 27")).isEqualTo("0013A200.405D8027");
        assertThat(Parser.render4x4("00 13 a2 00 f0 5d 80 27")).isEqualTo("0013A200.F05D8027");
    }
}
