package net.sf.dz3.device.actuator.servomaster;

/**
 * Base class for facilitating range and limit calibration for {@link ServoDamper}.
 *  
 * @param <E> Payload type.
 * @see {@link RangeCalibration}, {@link LimitCalibration}
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2009
 */
public class Calibration<E extends Number> {

    /**
     * Minimum value.
     */
    public final E min;
    
    /**
     * Maximum value.
     */
    public final E max;

    /**
     * Create an instance.
     * 
     * @param min Minimum value.
     * @param max Maximum value.
     */
    public Calibration(E min, E max) {

        this.min = min;
        this.max = max;
    }

    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(getClass().getSimpleName());
        sb.append('(').append(min).append("..").append(max).append(')');
        
        return sb.toString();
    }
}
