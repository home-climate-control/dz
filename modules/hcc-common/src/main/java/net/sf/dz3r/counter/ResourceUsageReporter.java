package net.sf.dz3r.counter;

/**
 * Resource usage reporter for {@link ResourceUsageCounter}.
 *
 * @param <T> Measured data type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface ResourceUsageReporter<T extends Comparable<T>> {
    void report(ResourceUsageCounter.State<T> state);
}
