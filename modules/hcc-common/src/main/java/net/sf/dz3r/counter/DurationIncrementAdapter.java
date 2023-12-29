package net.sf.dz3r.counter;

import java.time.Duration;

/**
 * Usage model converter for {@link Duration}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class DurationIncrementAdapter extends AbsoluteIncrementAdapter<Duration> {
    @Override
    protected boolean isZero(Duration d) {
        return d.isZero();
    }

    @Override
    protected boolean isMonotonous(Duration older, Duration newer) {
        return !diff(older, newer).isNegative();
    }

    @Override
    protected Duration diff(Duration older, Duration newer) {
        return newer.minus(older);
    }
}
