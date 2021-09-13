package net.sf.dz3r.model;

/**
 * A generic metrics collector.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public interface MetricsCollector {
    void connect(UnitDirector.Feed feed);
}
