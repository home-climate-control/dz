package net.sf.dz3.controller.pid;

import com.homeclimatecontrol.jukebox.conf.ConfigurableProperty;

/**
 * @author <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2009
 */
public interface PidControllerConfiguration extends AbstractPidControllerConfiguration {

  @ConfigurableProperty(
      propertyName = "I.time",
      description = "Integral component time span",
      defaultValue = "0"
  )
  void setIspan(long iSpan);

  @ConfigurableProperty(
      propertyName = "D.time",
      description = "Derivative component time span",
      defaultValue = "0"
  )
  void setDspan(long dSpan);

  @ConfigurableProperty(
      propertyName = "limit",
      description = "Anti-windup saturation limit, 0 is no limit",
      defaultValue = "0"
  )
  void setLimit(double limit);
}
