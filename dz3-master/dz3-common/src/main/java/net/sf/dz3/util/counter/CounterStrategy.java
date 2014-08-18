package net.sf.dz3.util.counter;

/**
 * Strategy for counting resource usage.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public interface CounterStrategy {

    /**
     * Calculate the number of units consumed.
     * 
     * @param timestamp Current timestamp.
     * @param value Value to consume.
     * 
     * @return The number of units consumed.
     */
    long consume(long timestamp, double value);
}
