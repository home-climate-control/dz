package net.sf.dz3r.device.z2m.common;

import com.homeclimatecontrol.hcc.TimeTool;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import static java.lang.Long.parseLong;

public class Z2MTool {

    /**
     * Parse the {@code last_seen} value per https://www.zigbee2mqtt.io/guide/configuration/all-settings.html#last-seen
     *
     * @param source Raw value obtained from the payload, could be {@code null}.
     *
     * @return Parsed value, or {@code null} if it is missing or unparseable.
     */
    public Instant parseLastSeen(Logger logger, String source) {

        // The value can be:

        // - missing if disabled (default Z2M behavior)
        // - ISO_8601
        // - ISO_8601_local
        // - epoch

        // All these must provide the same behavior - just like it was beore #354.

        try {

            return TimeTool.parseISO_8601(source);

        } catch (DateTimeParseException ex) {

            // Can't lose diagnostics, but can't afford to have them at WARN level - spammy

            if (!ex.getMessage().contains("could not be parsed at index 0")) {
                logger.debug("Failed to parse '{}' as ISO_8601", source, ex);
            }

            try {

                // Could be either epoch, or garbage, but not null and not blank
                return Instant.ofEpochMilli(parseLong(source.trim()));

            } catch (Throwable t) {
                logger.debug("Failed to parse '{}' as ISO_8601 or epoch, returning null", source);
                return null;
            }
        }
    }
}
