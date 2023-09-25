package net.sf.dz3r.model;

/**
 * A range.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class Range<T extends Comparable<T>> {

    public final T min;
    public final T max;

    public Range(T min, T max) {

        if (min.compareTo(max) >= 0) {
            throw new IllegalArgumentException("Invalid range " + min + ".." + max);
        }

        this.min = min;
        this.max = max;
    }

    public boolean contains(T value) {
        return min.compareTo(value) <= 0 && max.compareTo(value) >= 0;
    }

    @Override
    public String toString() {
        return min + ".." + max;
    }
}
