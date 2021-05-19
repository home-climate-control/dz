package net.sf.dz3.device.actuator.servomaster;

/**
 * Range calibration object for {@link ServoDamper}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
 */
public class RangeCalibration extends Calibration<Integer> {
    
    public RangeCalibration(int min, int max) {
        super(min, max);
    }
}
