package net.sf.dz3.util.counter;

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Usage counter proof of concept with no persistent state.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class TransientUsageCounter extends AbstractUsageCounter {

    /**
     * Create an instance with the default (time based usage) counter strategy and no storage keys.
     * 
     * @param name Human readable name for the user interface.
     * @param target What to count.
     * 
     * @throws IOException if things go sour.
     */
    public TransientUsageCounter(String name, DataSource<Double> target) throws IOException {

        super(name, target, null);
    }

    /**
     * Create an instance with no storage keys.
     * 
     * @param name Human readable name for the user interface.
     * @param counter Counter to use.
     * @param target What to count.
     * 
     * @throws IOException if things go sour.
     */
    public TransientUsageCounter(String name, CounterStrategy counter, DataSource<Double> target) throws IOException {

        super(name, counter, target, null);
    }

    /**
     * Create an instance.
     * 
     * @param name Human readable name for the user interface.
     * @param counter Counter to use.
     * @param target What to count.
     * @param storageKeys How to store the counter data.
     * 
     * @throws IOException if things go sour.
     */
    public TransientUsageCounter(String name, CounterStrategy counter, DataSource<Double> target, Object[] storageKeys) throws IOException {

        super(name, counter, target, storageKeys);
    }

    @Override
    protected void alert(long threshold, long current) {
        
        ThreadContext.push("alert@" + Integer.toHexString(hashCode()));
        
        try {

            if (threshold == 0) {
                logger.debug("Threshold not set");
                return;
            }

            long percent = (long)(getUsageRelative() * 100);
            
            String message = getName() + ": current usage " + percent + "%" + (percent > 100 ? " (OVERDUE)" : "");
            
            if (percent < 50) {
                logger.debug(message);
            } else if (percent < 80) {
                logger.info(message);
            } else if (percent < 100) {
                logger.warn(message);
            } else {
                logger.error(message);
            }
        
        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected CounterState load() throws IOException {

        // One hour limit
        return new CounterState(1000 * 60 * 60, 0);
    }

    @Override
    protected void save() throws IOException {

        ThreadContext.push("save@" + Integer.toHexString(hashCode()));

        try {

            // Do absolutely nothing except logging the current usage
            logger.info("Current usage: " + getUsageAbsolute() + "/" + getThreshold() + "(" + getUsageRelative() + ")");
        
        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        
        return new JmxDescriptor(
                "dz",
                "Resource Usage Counter",
                getName(),
                "Keeps track of resource usage and logs it");
    }

    @Override
    protected void doReset() throws IOException {
        
        // Do absolutely nothing
    }
}
