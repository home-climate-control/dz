package net.sf.dz3.util.counter;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import junit.framework.TestCase;

public class FileUsageCounterTest extends TestCase {

    public void testNonExistentFile() throws IOException {
        
        File f = createNonexistent();
        DataSource<Double> target = new DataSource<Double>() {

            @Override
            public void addConsumer(DataSink<Double> arg0) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void removeConsumer(DataSink<Double> arg0) {
                // TODO Auto-generated method stub
                
            }};
        
        FileUsageCounter counter = new FileUsageCounter("test", new TimeBasedUsage(), target, f);
        
        counter.save();
    }

    private File createNonexistent() {

        File tmp = new File(System.getProperty("java.io.tmpdir"));
        
        if (!tmp.exists() || !tmp.canWrite()) {
            
            fail(tmp + ": doesn't exist or can't write");
        }
        
        return new File(tmp, UUID.randomUUID().toString());
    }
}
