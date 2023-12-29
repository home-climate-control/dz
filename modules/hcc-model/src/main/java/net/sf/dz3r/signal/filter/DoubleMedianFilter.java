package net.sf.dz3r.signal.filter;


public class DoubleMedianFilter extends MedianFilter<Double> {

    public DoubleMedianFilter(int depth) {
        super(depth);
    }

    @Override
    protected Double average(Double d1, Double d2) {
        return (d1 + d2) / 2;
    }
}
