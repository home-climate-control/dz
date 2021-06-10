package net.sf.dz3.view.influxdb.v2;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@Disabled("Enable if you have InfluxDB running on localhost (or elsewhere, see the source)")
class InfluxDbLoggerTest {

    @Test
    void lifecycle() {
        assertThatCode(() -> {
            InfluxDbLogger l = new InfluxDbLogger("dz3.test.v2", "http://127.0.0.1:8086", null, null);
        }).doesNotThrowAnyException();
    }
}
