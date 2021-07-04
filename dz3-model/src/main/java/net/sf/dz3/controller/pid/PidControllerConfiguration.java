package net.sf.dz3.controller.pid;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;

/**
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2005-2021
 */
public interface PidControllerConfiguration extends AbstractPidControllerConfiguration {

  @JmxAttribute(description = "Integral component time span")
  long getIspan();

  /**
   * Set the controller integral span.
   *
   * @param iSpan Integral span, in milliseconds.
   */
  void setIspan(long iSpan);

  @JmxAttribute(description = "Derivative component time span")
  long getDspan();

  /**
   * Set the controller derivative span.
   *
   * @param dSpan Derivative span, in milliseconds.
   */
  void setDspan(long dSpan);
}
