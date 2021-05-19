package net.sf.dz3.device.actuator.servomaster;

/**
 * Limit calibration object for {@link ServoDamper}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
 */
public class LimitCalibration extends Calibration<Double> {
    
    public LimitCalibration(double min, double max) {
        super(min, max);
    }
}
