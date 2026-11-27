package net.sf.dz3r.device.z2m.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class Z2MToolTest {

    private final Logger logger = LogManager.getLogger();
    private final Z2MTool z2mtool = new Z2MTool();

    @Test
    void parseLastSeenNull() {
        assertThat(z2mtool.parseLastSeen(logger, null)).isNull();
    }

    @Test
    void parseLastSeenEpoch() {
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var millis = now.toEpochMilli();
        assertThat(z2mtool.parseLastSeen(logger, Long.toString(millis))).isEqualTo(now);
    }

    @Test
    void parseLastSeenGarbage() {
        assertThat(z2mtool.parseLastSeen(logger, "what?")).isNull();
    }
}
