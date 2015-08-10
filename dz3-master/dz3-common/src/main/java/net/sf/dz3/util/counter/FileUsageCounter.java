package net.sf.dz3.util.counter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import net.sf.jukebox.datastream.signal.model.DataSource;

import org.apache.log4j.NDC;

/**
 * Usage counter storing the state into a file.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected CounterState load() throws IOException {
        
        NDC.push("load");
        
        try {
            
            Object[] storageKeys = getStorageKeys();
            
            if (storageKeys == null) {
                throw new IllegalArgumentException("null storageKeys");
            }
            
            if (storageKeys.length == 0) {
                throw new IllegalArgumentException("empty storageKeys");
            }
            
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

            BufferedReader br = new BufferedReader(new FileReader(persistentStorage));

            Long threshold = null;
            Long current = null;

            while (true) {

                String line = br.readLine();
                
                if (line == null) {
                    
                    // End of file
                    
                    br.close();
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
                    
                    br.close();
                    throw new IOException("Failed to parse line '" + line + "' out of " + persistentStorage.getCanonicalPath());
                }
            }
            
            if (threshold == null) {
                throw new IOException("No '" + CF_THRESHOLD + "=NN' found in " + persistentStorage.getCanonicalPath());
            }

            if (current == null) {
                throw new IOException("No '" + CF_CURRENT +"=NN' found in " + persistentStorage.getCanonicalPath());
            }

            CounterState state = new CounterState(threshold.longValue(), current.longValue());

            logger.info("Loaded: " + state);

            return state;

        } finally {
            NDC.pop();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void save() throws IOException {

        NDC.push("save@" + Integer.toHexString(hashCode()));

        try {
            
            logger.debug(getUsageRelative() + "/" + getUsageAbsolute());

            File persistentStorage = (File) getStorageKeys()[0];
            File canonical = new File(persistentStorage.getCanonicalPath());
            
            canonical.getParentFile().mkdirs();
            
            PrintWriter pw = new PrintWriter(new FileWriter(canonical));
            
            pw.println("# Resource Usage Counter: " + getName());
            pw.println(CF_THRESHOLD + "=" + getThreshold());
            pw.println(CF_CURRENT + "=" + getUsageAbsolute());
            
            pw.close();
        
        } finally {
            NDC.pop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doReset() throws IOException {
        
        save();
    }
}
