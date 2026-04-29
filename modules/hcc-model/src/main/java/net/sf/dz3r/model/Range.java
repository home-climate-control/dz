package net.sf.dz3r.model;

/**
 * A range.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public record Range<T extends Comparable<T>>(T min, T max) {

    public Range {

        if (min.compareTo(max) >= 0) {
            throw new IllegalArgumentException("Invalid range " + min + ".." + max);
        }
    }

    public boolean contains(T value) {
        return min.compareTo(value) <= 0 && max.compareTo(value) >= 0;
    }

    @Override
    public String toString() {
        return min + ".." + max;
    }
}
