package com.homeclimatecontrol.hcc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static com.homeclimatecontrol.hcc.TimeTool.parseISO_8601;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TimeToolTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void parseISO_8601_null() {
        assertThat(parseISO_8601(null)).isNull();
    }

    @Test
    void parseISO_8601_empty() {
        assertThat(parseISO_8601("")).isNull();
    }

    @Test
    void parseISO_8601_blank() {
        assertThat(parseISO_8601(" ")).isNull();
    }

    @Test
    void parseISO_8601_garbage() {
        assertThatExceptionOfType(DateTimeParseException.class)
                .isThrownBy(() -> parseISO_8601("huh?"));
    }

    @Test
    void parseISO_8601_UTC() {
        var now = Instant.now();
        var source = now.toString();
        test(now, source);
    }

    @Test
    void parseISO_8601_local() {
        var now = Instant.now();
        var source = now
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        test(now, source);
    }

    private void test(Instant timestamp, String presentation) {
        logger.info("presentation: {}", presentation);
        assertThat(parseISO_8601(presentation)).isEqualTo(timestamp);
    }
}
