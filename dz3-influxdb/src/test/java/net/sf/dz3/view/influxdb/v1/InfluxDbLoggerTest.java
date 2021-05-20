package net.sf.dz3.view.influxdb.v1;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.fail;

/**
 * VT: FIXME: Get smarter about running non-unit tests - pipeline will suffer from things like this
 */
@Disabled("Enable if you have InfluxDB running on localhost (or elsewhere, see the source)")
public class InfluxDbLoggerTest {

    @Test
    public void testLogger() throws InterruptedException {

        ThreadContext.push("testLogger");

        try {
            
            String instance = "dz3.test";

            Set<DataSource<Integer>> producers = new HashSet<>();
            DataBroadcaster<Integer> b = new DataBroadcaster<>();

            producers.add(b);

            InfluxDbLogger<Integer> l = new InfluxDbLogger<>(producers, instance, "http://127.0.0.1:8086", null, null);
            
            try {

                if (!l.start().waitFor()) {
                    fail("InfluxDbLogger failed to start, see the logs for the cause");
                }
                
                b.broadcast(new DataSample<Integer>("test-source", "test-signature", new Random().nextInt(), null));
            } finally {
                l.stop();
            }

        } finally {
            ThreadContext.pop();
        }
    }
}
