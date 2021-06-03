package net.sf.dz3.util.counter;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;

class FileUsageCounterTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    void nullName() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FileUsageCounter(null, null, null, null))
                .withMessage("name can't be null");
    }

    @Test
    void nullCounter() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FileUsageCounter("name", null, null, null))
                .withMessage("counter can't be null");
    }

    @Test
    void nullStorage() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FileUsageCounter("name", new TimeBasedUsage(), null, null))
                .withMessage("persistentStorage can't be null");
    }

    @Test
    void nullTarget() {

        File f = createNonexistentDirect();

        try {

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new FileUsageCounter("name", new TimeBasedUsage(), null, f))
                    .withMessage("null target doesn't make sense");

        } finally {
            assertThat(f.delete()).isTrue();
        }
    }

    /**
     * Make sure {@link FileUsageCounter#reset()} works.
     */
    @Test
    void testReset() throws IOException, InterruptedException {

        ThreadContext.push("testReset");

        try {

            File f = createNonexistentDirect();
            FileUsageCounter counter = new FileUsageCounter("test", new TimeBasedUsage(), createTarget(), f);
            long delay = 100;

            counter.consume(new DataSample<>("source", "signature", 1d, null));
            Thread.sleep(delay); // NOSONAR Unnecessary complication
            counter.consume(new DataSample<>("source", "signature", 1d, null));
            counter.consume(new DataSample<>("source", "signature", null, new Error()));

            logger.debug("Consumed: {}", counter.getUsageAbsolute());

            // Can't really assert much, the timer is too inexact

            // Consumed value less than time passed???
            assertThat(counter.getUsageAbsolute()).isGreaterThan(delay);

            counter.reset();

            assertThat(counter.getUsageAbsolute()).isZero();

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    void consumeNull() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {

                    File f = createNonexistentDirect();
                    FileUsageCounter counter = new FileUsageCounter("test", new TimeBasedUsage(), createTarget(), f);
                    counter.consume(null);

                })
                .withMessage("signal can't be null");
    }

    @Test
    void existing() throws IOException, InterruptedException { // NOSONAR Failure will be obvious

        ThreadContext.push("existing");

        try {

            File f = File.createTempFile("counter", "");

            f.deleteOnExit();

            PrintWriter pw = new PrintWriter(new FileWriter(f));

            pw.println("# comment");
            pw.println("threshold=100");
            pw.println("current=0");

            pw.close();

            FileUsageCounter counter = new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), f);

            counter.consume(new DataSample<>("source", "signature", 1d, null));
            Thread.sleep(25); // NOSONAR Unnecessary complication

            // This will cause a debug message
            counter.consume(new DataSample<>("source", "signature", 1d, null));

            logger.debug("Consumed: " + counter.getUsageRelative());

            Thread.sleep(25); // NOSONAR Unnecessary complication

            // This will cause an info message
            counter.consume(new DataSample<>("source", "signature", 1d, null));

            Thread.sleep(30); // NOSONAR Unnecessary complication

            // This will cause a warning message
            counter.consume(new DataSample<>("source", "signature", 1d, null));

            Thread.sleep(30); // NOSONAR Unnecessary complication

            // This *will* cause an alert (error message)
            counter.consume(new DataSample<>("source", "signature", 1d, null));

            logger.debug("Consumed: " + counter.getUsageRelative());

        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    void noThreshold() throws IOException {

        File f = File.createTempFile("counter", "");

        f.deleteOnExit();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), f))
                .withMessage("No 'threshold=NN' found in " + f.getCanonicalPath());
    }

    @Test
    void noCurrent() throws IOException {

        File f = File.createTempFile("counter", "");

        f.deleteOnExit();

        PrintWriter pw = new PrintWriter(new FileWriter(f));

        pw.println("threshold=100");

        pw.close();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), f))
                .withMessage("No 'current=NN' found in " + f.getCanonicalPath());
    }

    @Test
    void badLine() throws IOException {

        File f = File.createTempFile("counter", "");

        f.deleteOnExit();

        PrintWriter pw = new PrintWriter(new FileWriter(f));

        pw.println("threshold-100");

        pw.close();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), f))
                .withMessage("Failed to parse line 'threshold-100' out of " + f.getCanonicalPath() + " (line 1)");
    }

    /**
     * Make sure directory can't be specified as persistent storage.
     */
    @Test
    void directory() {

        String tmp = System.getProperty("java.io.tmpdir");
        File dir = new File(tmp);

        assertThatIOException()
                .isThrownBy(() -> new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), dir))
                .withMessage(tmp + ": is a directory");
    }

    @Test
    void notRegularFile() {

        // Will not work on Windows, but who cares :)

        File devnull = new File("/dev/null");

        assertThatIOException()
                .isThrownBy(() -> new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), devnull))
                .withMessage(devnull + ": not a regular file");
    }

    @Test
    void notWritable() {

        // Will not work on Windows, but who cares :)
        // ...and if *this test fails, you're really doing it wrong

        File passwd = new File("/etc/passwd");

        assertThatIOException()
                .isThrownBy(() -> new FileUsageCounter("name", new TimeBasedUsage(), createTarget(), passwd))
                .withMessage(passwd + ": can't write");
    }

    /**
     * Make sure that the persistent storage file that doesn't exist is created.
     */
    @Test
    void nonExistentDirect() throws IOException {

        File f = createNonexistentDirect();
        FileUsageCounter counter = new FileUsageCounter("test", new TimeBasedUsage(), createTarget(), f);

        assertThat(f).doesNotExist();
        counter.save();
        assertThat(f).exists();

        assertThat(f.delete()).isTrue();
        assertThat(f).doesNotExist();
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

    @Test
    void nonExistentFileIndirect() throws IOException {

        File f = createNonexistentIndirect();
        FileUsageCounter counter = new FileUsageCounter("test", new TimeBasedUsage(), createTarget(), f);

        assertThat(f).doesNotExist();
        assertThat(f.getParentFile()).doesNotExist();

        counter.save();

        assertThat(f).exists();

        assertThat(f.delete()).isTrue();
        assertThat(f.getParentFile().delete()).isTrue();

        assertThat(f).doesNotExist();
        assertThat(f.getParentFile()).doesNotExist();
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

        return new DataSource<>() {

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

    /**
     * Make sure backup file gets created.
     *
     * @see <a href="https://github.com/home-climate-control/dz/issues/102">#102</a>
     */
    @Test
    void backup() throws IOException {

        ThreadContext.push("testReset");

        try {

            File tmp = new File(System.getProperty("java.io.tmpdir"));

            assertThat(tmp).exists();
            assertThat(tmp).canRead();
            assertThat(tmp).canWrite();

            File counterFile = new File(tmp, UUID.randomUUID().toString());
            File backupFile = new File(tmp, counterFile.getName() + "-");

            FileUsageCounter counter = new FileUsageCounter("test", new TimeBasedUsage(), createTarget(), counterFile);

            counter.consume(new DataSample<>("source", "signature", 1d, null));

            assertThat(counterFile).exists();
            assertThat(backupFile).doesNotExist();

            counter.consume(new DataSample<>("source", "signature", 1d, null));

            assertThat(counterFile).exists();
            assertThat(backupFile).exists();

        } finally {
            ThreadContext.pop();
        }
    }
}
