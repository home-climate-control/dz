package net.sf.dz3.controller.pid;

/**
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2005-2021
 */
public interface PidControllerConfiguration extends AbstractPidControllerConfiguration {

  /**
   * Set the controller integral span.
   *
   * @param iSpan Integral span, in milliseconds.
   */
  void setIspan(long iSpan);

  /**
   * Set the controller derivative span.
   *
   * @param dSpan Derivative span, in milliseconds.
   */
  void setDspan(long dSpan);
}
