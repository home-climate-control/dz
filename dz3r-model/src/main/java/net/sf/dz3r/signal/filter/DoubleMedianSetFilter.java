package net.sf.dz3r.signal.filter;

public class DoubleMedianSetFilter<P> extends MedianSetFilter<Double, P> {

    protected DoubleMedianSetFilter(int depth) {
        super(depth);
    }

    @Override
    protected Double average(Double d1, Double d2) {
        return (d1 + d2) / 2;
    }
}
