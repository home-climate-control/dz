package net.sf.dz3.device.model.impl;

import net.sf.dz3.device.model.HvacSignal;

import java.io.Serializable;

/**
 * Extended {@link net.sf.dz3.device.actuator.HvacController} signal
 * (includes the {@link net.sf.dz3.device.actuator.HvacDriver} status.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class HvacExtendedSignal implements Serializable {

    private static final long serialVersionUID = -5053515940880380050L;

    public final String unitName;
    public final String signature;
    public final HvacSignal controllerSignal;
    public final HvacDriverSignal driverSignal;

    public HvacExtendedSignal(String unitName, String signature, HvacSignal controllerSignal, HvacDriverSignal driverSignal) {

        this.unitName = unitName;
        this.signature = signature;
        this.controllerSignal = controllerSignal;
        this.driverSignal = driverSignal;
    }
}
