package net.sf.dz3.util.counter;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import junit.framework.TestCase;

public class FileUsageCounterTest extends TestCase {

    /**
     * Make sure that the persistent storage file that doesn't exist is created.
     */
    public void testNonExistentDirect() throws IOException {
        
        File f = createNonexistentDirect();
        FileUsageCounter counter = new FileUsageCounter("test", new TimeBasedUsage(), createTarget(), f);
        
        assertEquals("The file mustn't exist at this point yet", false, f.exists());
        counter.save();
        assertEquals("The file exist at this point", true, f.exists());
        
        f.delete();
        assertEquals("The file must have been deleted by now", false, f.exists());
    }

    /**
     * @return a file name in an existing directory.
     */
    private File createNonexistentDirect() {

        File tmp = new File(System.getProperty("java.io.tmpdir"));
        
        if (!tmp.exists() || !tmp.canWrite()) {
            
            fail(tmp + ": doesn't exist or can't write");
        }
        
        return new File(tmp, UUID.randomUUID().toString());
    }

    public void testNonExistentFileIndirect() throws IOException {
        
        File f = createNonexistentIndirect();
        FileUsageCounter counter = new FileUsageCounter("test", new TimeBasedUsage(), createTarget(), f);
        
        assertEquals("The file mustn't exist at this point yet", false, f.exists());
        assertEquals("The file parent directory mustn't exist at this point yet", false, f.getParentFile().exists());
        counter.save();
        assertEquals("The file exist at this point", true, f.exists());

        f.delete();
        f.getParentFile().delete();

        assertEquals("The file must have been deleted by now", false, f.exists());
        assertEquals("The file parent directory must have been deleted by now", false, f.getParentFile().exists());
    }

    /**
     * @return a file name in a non-existing directory.
     */
    private File createNonexistentIndirect() {

        File tmp = new File(System.getProperty("java.io.tmpdir"));
        
        if (!tmp.exists() || !tmp.canWrite()) {
            
            fail(tmp + ": doesn't exist or can't write");
        }
        
        File indirect = new File(tmp, UUID.randomUUID().toString());
        
        return new File(indirect, "oops");
    }
    
    private DataSource<Double> createTarget() {
        
        return new DataSource<Double>() {

            @Override
            public void addConsumer(DataSink<Double> arg0) {
                // Do absolutely nothing
            }

            @Override
            public void removeConsumer(DataSink<Double> arg0) {
                // Do absolutely nothing
            }
        };
    }
}
