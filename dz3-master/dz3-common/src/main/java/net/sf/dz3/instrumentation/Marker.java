package net.sf.dz3.instrumentation;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * An object to keep track of time spent on something in a convenient manner.
 * 
 * Usage pattern (just like {@link NDC}:
 * 
 * {@code
 * 
 * NDC.push("whatever");
 * Marker m = new Marker("something complicated");
 * 
 * try {
 *     ... do something
 *  
 * } finally {
 * 
 *     // NOTE: Put m.close() BEFORE NDC.pop(), not after, to preserve the context
 *     m.close();
 *     NDC.pop();
 * }
 * 
 */
public class Marker {

    private final Logger logger = Logger.getLogger(getClass());
    
    /**
     * Message to be printed in the log when the marker is closed.
     */
    private final String marker;
    
    /**
     * Level to print marker messages with.
     */
    private final Level level;
    
    /**
     * Timekeeper.
     */
    private StopWatch stopWatch;
    
    /**
     * Redundant invocation preventer.
     */
    private boolean closed = false;
    
    /**
     * Create an instance and start the {@link #stopWatch timer}.
     * 
     * @param marker Message to print when the marker is closed.
     * It would be a good idea not to use parentheses in this string, for they
     * will break automated processing.
     * 
     * @param level Level to print marker messages with.
     */
    public Marker(String marker, Level level) {
    
        if (marker == null) {
            throw new IllegalArgumentException("Marker can't be null");
        }
        
        if (level == null) {
            throw new IllegalArgumentException("Level can't be null");
        }
        
        this.marker = marker;
        this.level = level;
        
        stopWatch = new StopWatch();
        stopWatch.start();

        printStartMarker();
    }

    /**
     * Create an instance at {@link Level#DEBUG DEBUG} level and start the {@link #stopWatch timer}.
     * 
     * @param marker Message to print when the marker is closed.
     * It would be a good idea not to use parentheses in this string, for they
     * will break automated processing.
     */
    public Marker(String marker) {
        
        this(marker, Level.DEBUG);
    }

    protected void printStartMarker() {

        // Mark the beginning of the timed section

        StringBuilder sb = new StringBuilder();

        getSignature(sb);
        sb.append(marker).append(") started");

        logger.log(level, sb.toString());
    }
    
    /**
     * Print the diagnostic message and keep going.
     * 
     * @param checkpointMessage Message to print.
     * 
     * @return Time elapsed since creation of this marker instance, in milliseconds.
     */
    public final long checkpoint(String checkpointMessage) {
        
        StringBuilder sb = new StringBuilder();
        
        getSignature(sb);
        sb.append(marker);
        sb.append(") checkpoint '" + checkpointMessage + "' reached at ");

        printTimeMarker(sb, stopWatch);
        
        logger.log(level, sb.toString());
        
        return stopWatch.getTime();
    }
    
    /**
     * Close the interval and print the diagnostic message.
     * 
     * You can only {@code close()} the marker once.
     * 
     * @return Time elapsed since creation of this marker instance, in milliseconds.
     */
    public final long close() {
        
        if (closed) {
            StringBuilder sb = new StringBuilder();
            
            getSignature(sb);
            sb.append("already closed)");
            throw new IllegalStateException(sb.toString());
        }
        
        stopWatch.stop();

        closed = true;
        
        StringBuilder sb = new StringBuilder();
        
        getSignature(sb);
        sb.append(marker);
        sb.append(") completed in ");

        printTimeMarker(sb, stopWatch);
        
        logger.log(level, sb.toString());
        
        return stopWatch.getTime();
    }

    protected void printTimeMarker(StringBuilder sb, StopWatch stopWatch) {
        sb.append(stopWatch.getTime()).append(" ms (").append(stopWatch.toString()).append(")");
    }
    
    /**
     * Get the unique marker signature.
     * 
     * @param sb Buffer to append the signature to.
     */
    private void getSignature(StringBuilder sb) {
        sb.append("marker");
        sb.append('@').append(Integer.toHexString(Thread.currentThread().hashCode()));
        sb.append('#').append(Integer.toHexString(hashCode()));
        sb.append(": (");
    }
    
    protected void finalize() {
        
        if (!closed) {

            StringBuilder sb = new StringBuilder();

            getSignature(sb);
            sb.append(marker);
            sb.append(") RUNAWAY INSTANCE, NOT close()d");

            logger.error(sb.toString());
        }
    }
}
