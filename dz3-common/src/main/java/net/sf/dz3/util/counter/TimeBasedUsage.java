package net.sf.dz3.util.counter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple time based usage counter.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
public class TimeBasedUsage implements CounterStrategy {
    
    private final Logger logger = LogManager.getLogger(getClass());
    
    private long last;
    private boolean running = false;
    
    public TimeBasedUsage() {
    
        this(System.currentTimeMillis());
    }

    public TimeBasedUsage(long timestamp) {
    
        this.last = timestamp;
    }

    /**
     * Interpret {@code value} as "running or not" and use {@code timestamp}
     * to calculate usage.
     * 
     * @return milliseconds consumed.
     */
    @Override
    public synchronized long consume(long timestamp, double value) {
        
        if (timestamp < last) {
            
            logger.warn("Can't go back in time - timestamp ("
                    + timestamp + " is " + (last - timestamp) + "ms less than last known (" + last + "), sample ignored");
            
            return 0;
        }

        long result = running ? timestamp - last : 0;
        
        last = timestamp;
        running = value > 0;
        
        return result;
    }
}
