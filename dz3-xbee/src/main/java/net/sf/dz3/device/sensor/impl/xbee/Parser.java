package net.sf.dz3.device.sensor.impl.xbee;

import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.rapplogic.xbee.api.XBeeAddress64;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2010-2020
 */
public class Parser {

    private static final Logger logger = LogManager.getLogger(Parser.class);


    /**
     * Parse a loosely formatted 64 bit address into a form that is understood by AR's constructor.
     *
     * @param rawAddress Raw address to parse. You may write it any way you want (feel free to insert
     * spaces and dots), just make sure you specify all bytes, even leading zeros.
     *
     * @return Well formed 64 bit XBee address.
     */
    public static XBeeAddress64 parse(String rawAddress) {

        ThreadContext.push("parse");

        try {

            logger.debug("raw address: '{}'", rawAddress);

            String denseAddress = compress(rawAddress);

            logger.debug("dense address: {}", denseAddress);

            if (denseAddress.length() != 16) {

                throw new IllegalArgumentException("Bad address, dense form: '" + denseAddress + "'");
            }

            // Now let's fill it with spaces

            StringBuilder sb = new StringBuilder();

            for (int offset = 0; offset < denseAddress.length(); offset++) {

                if (offset != 0) {

                    sb.append(' ');
                }

                sb.append(denseAddress.charAt(offset++));
                sb.append(denseAddress.charAt(offset));
            }

            String result = sb.toString();

            logger.debug("Rapp address: {}", result);

            return new XBeeAddress64(result);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Remove spaces and dots from the XBee hardware address string given.
     *
     * @param rawAddress XBee hardware address as a string.
     *
     * @return XBee hardware address with spaces and dots removed.
     */
    public static String compress(String rawAddress) {

        StringBuilder sb = new StringBuilder();

        for (StringTokenizer st = new StringTokenizer(rawAddress, " ."); st.hasMoreTokens(); ) {

            sb.append(st.nextToken());
        }

        return sb.toString();
    }

    /**
     * Format the 64 bit XBee address as "XXXX.XXXX".
     *
     * @param xbeeAddress XBee address object to get the address to format from.
     *
     * @return "XXXX.XXXX" formatted string.
     */
    public static String render4x4(XBeeAddress64 xbeeAddress) {

        StringBuilder sb = new StringBuilder();
        int[] address = xbeeAddress.getAddress();

        for (int offset = 0; offset < address.length; offset++) {

            if (offset == 4) {
                sb.append('.');
            }

            if ((address[offset] & 0xF0) == 0) {
                sb.append('0');
            }

            sb.append(Integer.toHexString(address[offset]).toUpperCase());
        }

        return sb.toString();
    }

    /**
     * Format the free form 64 bit XBee address as "XXXX.XXXX".
     *
     * @param address Free form 64 bit XBee address.
     *
     * @return "XXXX.XXXX" formatted string.
     */
    public static String render4x4(String address) {

        return render4x4(parse(address));
    }
}
