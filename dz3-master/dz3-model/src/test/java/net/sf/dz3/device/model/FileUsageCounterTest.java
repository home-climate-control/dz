package net.sf.dz3.device.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import net.sf.dz3.device.model.impl.UnitSignalSplitter;
import net.sf.dz3.util.counter.FileUsageCounter;
import net.sf.dz3.util.counter.TimeBasedUsage;
import net.sf.jukebox.datastream.signal.model.DataSample;

/**
 * Test case for {@link FileUsageCounter}. The difference between {@link net.sf.dz3.util.counter.FileUsageCounterTest}
 * and this one is that the other one tests {@link FileUsageCounter} itself, and this one tests integration with
 * {@link Unit} (or, rather, {@link UnitSignalSplitter).
 */
public class FileUsageCounterTest {

    @Test
    public void testConsume() throws IOException, InterruptedException {

        UnitSignalSplitter uss = new UnitSignalSplitter();
        FileUsageCounter c = new FileUsageCounter("Usage test", new TimeBasedUsage(), uss, getTempName());
        UnitSignal sample = new UnitSignal(0.1, true, 100);
        final long delay = 200L;

        // This sample will just prime the counter, but not add to it
        uss.consume(new DataSample<>("test-unit", "signature", sample, null));
        assertEquals(0, c.getUsageAbsolute());
        
        Thread.sleep(delay);
        
        // This one will add approximately delay milliseconds to it
        uss.consume(new DataSample<>("test-unit", "signature", sample, null));
        
        assertTrue(c.getUsageAbsolute() >= delay);
    }
    
    private File getTempName() throws IOException {
        
        File result = File.createTempFile("usage-", ".counter", new File(System.getProperty("java.io.tmpdir")));
        
        // Need to delete it because all we need is a name. Clash risk is non-existent
        result.delete();
        
        return result;
    }
}
