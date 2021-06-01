package net.sf.dz3.controller.pid;

import net.sf.dz3.controller.DataSet;

/**
 * Data set supporting the differential calculation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2015
 */
public interface DifferentialSet {

    /**
     * @see DataSet#append(long, Object)
     */
    void append(long millis, Double value);

  /**
   * Get the differential starting with the first data element available and
   * ending with the last data element available.
   * <p>
   * Differentiation time must have been taken care of by {@link
   * DataSet#expire expiration}.
   *
   * @return A differential value.
   */
  double getDifferential();
}
