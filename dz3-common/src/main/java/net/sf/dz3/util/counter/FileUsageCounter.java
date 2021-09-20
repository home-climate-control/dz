package net.sf.dz3.util.counter;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import org.apache.logging.log4j.ThreadContext;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * Usage counter storing the state into a file.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class FileUsageCounter extends TransientUsageCounter {

    private static final String CF_THRESHOLD = "threshold";
    private static final String CF_CURRENT = "current";

    /**
     * Create an instance.
     *
     * @param name Human readable name for the user interface.
     * @param counter Counter to use.
     * @param target What to count.
     * @param persistentStorage File to store the counter data into.
     *
     * @throws IOException if things go sour.
     */
    public FileUsageCounter(String name, CounterStrategy counter, DataSource<Double> target, File persistentStorage) throws IOException {
        super(name, counter, target, new Object [] { persistentStorage });
    }

    @Override
    protected CounterState load() throws IOException {

        ThreadContext.push("load");

        try {

            Object[] storageKeys = getStorageKeys();

            File persistentStorage = (File) storageKeys[0];

            if (persistentStorage == null) {
                throw new IllegalArgumentException("persistentStorage can't be null");
            }

            logger.info("Loading {}", persistentStorage);

            if (!persistentStorage.exists()) {

                logger.warn("{} doesn't exist, will initialize", persistentStorage);
                return new CounterState(0, 0);
            }

            checkSanity(persistentStorage);

            try (LineNumberReader lnr = new LineNumberReader(new FileReader(persistentStorage))) {

                Long threshold = null;
                Long current = null;

                while (true) {

                    String line = lnr.readLine();

                    if (line == null) {

                        // End of file
                        break;
                    }

                    if (line.startsWith("#")) {
                        // That's a comment
                        continue;
                    }

                    StringTokenizer st = new StringTokenizer(line, "=");

                    try {

                        String key = st.nextToken();
                        var value = Long.parseLong(st.nextToken());

                        if (CF_THRESHOLD.equals(key)) {
                            threshold = value;
                        }

                        if (CF_CURRENT.equals(key)) {
                            current = value;
                        }

                    } catch (Throwable t) { // NOSONAR Consequences have been considered
                        throw new IllegalArgumentException("Failed to parse line '" + line + "' out of " + persistentStorage.getCanonicalPath() + " (line " + lnr.getLineNumber() + ")");
                    }
                }

                if (threshold == null) {
                    throw new IllegalArgumentException("No '" + CF_THRESHOLD + "=NN' found in " + persistentStorage.getCanonicalPath());
                }

                if (current == null) {
                    throw new IllegalArgumentException("No '" + CF_CURRENT +"=NN' found in " + persistentStorage.getCanonicalPath());
                }

                CounterState state = new CounterState(threshold, current);

                logger.info("Loaded: {}", state);

                return state;
            }

        } finally {
            ThreadContext.pop();
        }
    }

    private void checkSanity(File persistentStorage) throws IOException {

        if (persistentStorage.isDirectory()) {
            throw new IOException(persistentStorage + ": is a directory");
        }

        if (!persistentStorage.canWrite()) {
            throw new IOException(persistentStorage + ": can't write");
        }

        if (!persistentStorage.isFile()) {
            throw new IOException(persistentStorage + ": not a regular file");
        }
    }

    @Override
    protected synchronized void save() throws IOException {

        ThreadContext.push("save@" + Integer.toHexString(hashCode()));

        try {

            logger.debug("{}/{}", getUsageRelative(), getUsageAbsolute());

            File persistentStorage = (File) getStorageKeys()[0];
            File canonical = new File(persistentStorage.getCanonicalPath());

            if (canonical.getParentFile().mkdirs()) {
                logger.info("Created {}", canonical);
            }

            // Now, careful... https://github.com/home-climate-control/dz/issues/102

            // If we just try to open the file and write into it, and get hit by an
            // interrupt at this very moment before the file system gets flushed (yes, it
            // does happen), we end up with zero length file which breaks everything on next load()

            // To counter that, let's write into a temporary file first, then flip the old counter file into a backup,
            // and the temp file into the counter file

            File temp = new File(canonical.getParent(), canonical.getName() + "+");
            File backup = new File(canonical.getParent(), canonical.getName() + "-");

            try (PrintWriter pw = new PrintWriter(new FileWriter(temp))) {

                pw.println("# Resource Usage Counter: " + getName());
                pw.println(CF_THRESHOLD + "=" + getThreshold());
                pw.println(CF_CURRENT + "=" + getUsageAbsolute());
            }

            if (canonical.exists() && !canonical.renameTo(backup)) {
                throw new IOException("failed to rename " + canonical + " to " + backup);
            }

            if (!temp.renameTo(canonical)) {
                throw new IOException("failed to rename " + temp + " to " + canonical);
            }

        } finally {
            ThreadContext.pop();
        }
    }
}
