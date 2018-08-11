package net.sf.dz3.util.counter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import org.apache.logging.log4j.ThreadContext;

import net.sf.jukebox.datastream.signal.model.DataSource;

/**
 * Usage counter storing the state into a file.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class FileUsageCounter extends TransientUsageCounter {
    
    private final static String CF_THRESHOLD = "threshold";
    private final static String CF_CURRENT = "current";
    
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
            
            logger.info("Loading " + persistentStorage);
            
            if (persistentStorage.isDirectory()) {
                throw new IOException(persistentStorage + ": is a directory");
            }
            
            if (!persistentStorage.exists()) {
                
                logger.warn(persistentStorage + " doesn't exist, will initialize");
                return new CounterState(0, 0);
            }

            if (!persistentStorage.canWrite()) {
                throw new IOException(persistentStorage + ": can't write");
            }
            
            if (!persistentStorage.isFile()) {
                throw new IOException(persistentStorage + ": not a regular file");
            }

            LineNumberReader lnr = new LineNumberReader(new FileReader(persistentStorage));
            
            try {

                Long threshold = null;
                Long current = null;

                while (true) {

                    String line = lnr.readLine();

                    if (line == null) {

                        // End of file

                        lnr.close();
                        break;
                    }

                    if (line.startsWith("#")) {
                        // That's a comment
                        continue;
                    }

                    StringTokenizer st = new StringTokenizer(line, "=");

                    try {

                        String key = st.nextToken();
                        Long value = Long.parseLong(st.nextToken());

                        if ("threshold".equals(key)) {
                            threshold = value;
                        }

                        if ("current".equals(key)) {
                            current = value;
                        }

                    } catch (Throwable t) {

                        lnr.close();
                        throw new IllegalArgumentException("Failed to parse line '" + line + "' out of " + persistentStorage.getCanonicalPath() + " (line " + lnr.getLineNumber() + ")");
                    }
                }

                if (threshold == null) {
                    throw new IllegalArgumentException("No '" + CF_THRESHOLD + "=NN' found in " + persistentStorage.getCanonicalPath());
                }

                if (current == null) {
                    throw new IllegalArgumentException("No '" + CF_CURRENT +"=NN' found in " + persistentStorage.getCanonicalPath());
                }

                CounterState state = new CounterState(threshold.longValue(), current.longValue());

                logger.info("Loaded: " + state);

                return state;

            } finally {
            
                lnr.close();
            }

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected synchronized void save() throws IOException {

        ThreadContext.push("save@" + Integer.toHexString(hashCode()));

        try {
            
            logger.debug(getUsageRelative() + "/" + getUsageAbsolute());

            File persistentStorage = (File) getStorageKeys()[0];
            File canonical = new File(persistentStorage.getCanonicalPath());
            
            if (canonical.getParentFile().mkdirs()) {
                logger.info("Created " + canonical);
            };
            
            PrintWriter pw = new PrintWriter(new FileWriter(canonical));
            
            pw.println("# Resource Usage Counter: " + getName());
            pw.println(CF_THRESHOLD + "=" + getThreshold());
            pw.println(CF_CURRENT + "=" + getUsageAbsolute());
            
            pw.close();
        
        } finally {
            ThreadContext.pop();
        }
    }
}
