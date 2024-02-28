package net.sf.dz3r.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.format.DateTimeParseException;

public class DurationParser {

    private final Logger logger = LogManager.getLogger();

    public Duration parse(String source) {

        try {
            return Duration.parse(source);
        } catch (DateTimeParseException ex) {
            try {
                logger.warn("'{}' doesn't parse, trying PT{}", source, source);
                return Duration.parse("PT" + source);
            } catch (DateTimeParseException ex2) {
                try {
                    logger.warn("'PT{}' doesn't parse, trying {} hours", source, source);
                    return Duration.ofHours(Long.parseLong(source));
                } catch (NumberFormatException ex3) {

                    // Enough is enough
                    logger.warn("Failed to parse '{}' as a) Duration, b) PT$duration, c) hours; giving up and returning 0", source);
                    return Duration.ZERO;
                }
            }
        }
    }
}
