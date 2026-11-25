package net.sf.dz3r.device.z2m.v1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class Z2MJsonListenerTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void parseLastSeenEpoch() {
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var millis = now.toEpochMilli();
        assertThat(Z2MJsonListener.parseLastSeen(logger, Long.toString(millis))).isEqualTo(now);
    }

    @Test
    void parseLastSeenGarbage() {
        assertThat(Z2MJsonListener.parseLastSeen(logger, "what?")).isNull();
    }
}
