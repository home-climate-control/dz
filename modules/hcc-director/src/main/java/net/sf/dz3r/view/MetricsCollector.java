package net.sf.dz3r.view;

import net.sf.dz3r.model.UnitDirector;

/**
 * A generic metrics collector.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface MetricsCollector {
    void connect(String unitId, UnitDirector.Feed feed);
}
