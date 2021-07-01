package net.sf.dz3.controller.pid;

/**
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2005-2021
 */
public interface AbstractPidControllerConfiguration {

    void setP(double p);
    void setI(double i);
	void setD(double d);

	/**
	 * Set the controller saturation limit.
	 *
	 * @param saturationLimit Saturation limit to set.
	 */
    void setLimit(double saturationLimit);
}
