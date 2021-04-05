package net.sf.dz3.controller.pid;

import com.homeclimatecontrol.jukebox.conf.ConfigurableProperty;

/**
 * @author <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2009
 */
public interface AbstractPidControllerConfiguration {

    @ConfigurableProperty(
	    propertyName = "P",
	    description = "Proportional component",
	    defaultValue = "0"
		)
    public abstract void setP(double p);

    @ConfigurableProperty(
            propertyName = "I.weight",
	    description = "Integral component weight",
	    defaultValue = "0"
		)
    public abstract void setI(double i);

    @ConfigurableProperty(
            propertyName = "I.limit",
	    description = "Integral component saturation limit",
	    defaultValue = "0"
		)
    public abstract void setLimit(double saturationLimit);

    @ConfigurableProperty(
            propertyName = "D.weight",
	    description = "Derivative component weight",
	    defaultValue = "0"
		)
    public abstract void setD(double d);
}