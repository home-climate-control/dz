package net.sf.dz3.view.influxdb.v1;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;

import static org.assertj.core.api.Assertions.fail;

@Disabled("Enable if you have InfluxDB running on localhost (or elsewhere, see the source)")
class InfluxDbLoggerTest {

    @Test
    void testLogger() throws InterruptedException {

        ThreadContext.push("testLogger");

        try {

            var instance = "dz3.test.v1";

            var producers = new HashSet<DataSource<Integer>>();
            var broadcaster = new DataBroadcaster<Integer>();

            producers.add(broadcaster);

            InfluxDbLogger<Integer> l = new InfluxDbLogger<>(producers, instance, "http://127.0.0.1:8086", null, null);

            try {

                if (!l.start().waitFor()) {
                    fail("InfluxDbLogger failed to start, see the logs for the cause");
                }

                broadcaster.broadcast(new DataSample<Integer>("test-source", "test-signature", new Random().nextInt(), null));
            } finally {
                l.stop();
            }

        } finally {
            ThreadContext.pop();
        }
    }
}
