package net.sf.dz3.util.counter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.datastream.signal.model.DataSource;
import junit.framework.TestCase;

public class FileUsageCounterTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());
    
    /**
     * Make sure no null arguments are accepted.
     */
    public void testNullArgs() throws IOException {
    
        try {

            new FileUsageCounter(null, null, null, null);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "name can't be null", ex.getMessage());
        }

        try {

            new FileUsageCounter("name", null, null, null);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "counter can't be null", ex.getMessage());
        }

        try {

            new FileUsageCounter("name", new TimeBasedUsage(), null, null);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "persistentStorage can't be null", ex.getMessage());
        }

        File f = createNonexistentDirect();

        try {

            new FileUsageCounter("name", new TimeBasedUsage(), null, f);
            fail("Should've failed by now");
            
        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "null target doesn't make sense", ex.getMessage());

        } finally {
            f.delete();
        }
    }
    
    /**
     * Make sure {@link FileUsageCounter#reset()} works.
     */
    public void testReset() throws IOException, InterruptedException {
        
        NDC.push("testReset");
        
        try {
        
            File f = createNonexistentDirect();
            FileUsageCounter counter = new FileUsageCounter("test", new TimeBasedUsage(), createTarget(), f);
            long delay = 100;

            counter.consume(new DataSample<Double>("source", "signature", 1d, null));
            Thread.sleep(delay);
            counter.consume(new DataSample<Double>("source", "signature", 1d, null));
            
            logger.debug("Consumed: " + counter.getUsageAbsolute());
            
            // Can't really assert much, the timer is too inexact
            
            assertTrue("Consumed value less than time passed???", counter.getUsageAbsolute() > delay);

            counter.reset();
            
            assertEquals("Wrong value after reset", 0, counter.getUsageAbsolute());

        } finally {
            NDC.pop();
        }
    }
    
    public void testExisting() throws IOException {
        
        File f = File.createTempFile("counter", "");
        
        f.deleteOnExit();

        PrintWriter pw = new PrintWriter(new FileWriter(f));

        pw.println("# comment");
        pw.println("threshold=100");
        pw.println("current=0");
        
        pw.close();
        
        new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), f);
    }
    
    public void testNoThreshold() throws IOException {
        
        File f = File.createTempFile("counter", "");
        
        f.deleteOnExit();

        try {
            
            new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), f);
            
        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "No 'threshold=NN' found in " + f.getCanonicalPath(), ex.getMessage());
        }
    }

    public void testNoCurrent() throws IOException {
        
        File f = File.createTempFile("counter", "");
        
        f.deleteOnExit();

        PrintWriter pw = new PrintWriter(new FileWriter(f));

        pw.println("threshold=100");
        
        pw.close();
        
        try {
            
            new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), f);
            
        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "No 'current=NN' found in " + f.getCanonicalPath(), ex.getMessage());
        }
    }

    public void testBadLine() throws IOException {
        
        File f = File.createTempFile("counter", "");
        
        f.deleteOnExit();

        PrintWriter pw = new PrintWriter(new FileWriter(f));

        pw.println("threshold-100");
        
        pw.close();
        
        try {
            
            new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), f);
            
        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "Failed to parse line 'threshold-100' out of " + f.getCanonicalPath() + " (line 1)", ex.getMessage());
        }
    }

    /**
     * Make sure directory can't be specified as persistent storage.
     */
    public void testDirectory() throws IOException {
        
        String tmp = System.getProperty("java.io.tmpdir");
        File dir = new File(tmp);
        
        try {
            
            new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), dir);
            fail("Should've failed by now");
            
        } catch (IOException ex) {
            assertEquals("Wrong exception message", tmp + ": is a directory", ex.getMessage());
        }
    }

    public void testNotRegularFile() throws IOException {
        
        // Will not work on Windows, but who cares :)
        
        File devnull = new File("/dev/null");
        
        try {
            
            new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), devnull);
            fail("Should've failed by now");
            
        } catch (IOException ex) {
            assertEquals("Wrong exception message", devnull + ": not a regular file", ex.getMessage());
        }
    }

    public void testNotWritable() throws IOException {
        
        // Will not work on Windows, but who cares :)
        // ...and if *this test fails, you're really doing it wrong
        
        File passwd = new File("/etc/passwd");
        
        try {
            
            new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), passwd);
            fail("Should've failed by now");
            
        } catch (IOException ex) {
            assertEquals("Wrong exception message", passwd + ": can't write", ex.getMessage());
        }
    }

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
