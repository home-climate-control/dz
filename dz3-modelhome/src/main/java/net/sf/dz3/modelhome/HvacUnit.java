package net.sf.dz3.modelhome;

import net.sf.dz3.device.model.HvacMode;

/**
 * A generic HVAC unit.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2012
 */
public class HvacUnit extends AmbientTemperatureAware {
    
    /**
     * Current HVAC mode.
     */
    private HvacMode mode;
    
    /**
     * This unit's output, in watts.
     * 
     * @see http://en.wikipedia.org/wiki/Watt
     */
    private double capacity;
    
    /**
     * Rate of performance degradation with temperature increase. Units to be defined.
     */
    private double heatTolerance;
    
    /**
     * 
     * @param mode Initial mode.
     * @param capacity Unit's capacity, in watts (joules per second).
     * @param heatTolerance see {@link #heatTolerance}
     * @param ambientTemperature, in °C.
     */
    public HvacUnit(HvacMode mode, double capacity, double heatTolerance, double ambientTemperature) {
        super(ambientTemperature);
        
        setMode(mode);
        setCapacity(capacity);
        setHeatTolerance(heatTolerance);
    }

    /**
     * Set new value for {@link #mode}.
     * 
     * @param mode New value for {@link #mode}.
     */
    public void setMode(HvacMode mode) {
        
        if (mode == null) {
            throw new IllegalArgumentException("mode can't be null");
        }
        
        this.mode = mode;
    }
    
    /**
     * Set new value for {@link #capacity}.
     * 
     * @param mode New value for {@link #capacity}, in watts.
     */
    public void setCapacity(double capacity) {
        
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        
        this.capacity = capacity;
    }
    
    /**
     * Set a new value for {@link #heatTolerance}.
     * 
     * @param tolerance New value for {@link #heatTolerance}.
     */
    public void setHeatTolerance(double tolerance) {
        
        // VT: FIXME: Disabled until units are figured out
//        if (tolerance < 0 || tolerance > 1) {
//            throw new IllegalArgumentException("Valid values are 0..1");
//        }
        
        this.heatTolerance = tolerance;
    }

    /**
     * Flow rate that this unit produces.
     * 
     * @return Energy produced, in watts (joules per second).
     */
    private double getProduction() {
        return capacity * mode.mode;
    }
    
    /**
     * Calculate the amount of energy produced in a given time interval.
     * 
     * @param internalTemperature Internal temperature at the start of the interval, in °C
     * @param millis Time interval, in milliseconds.
     * 
     * @return Energy produced, in joules.
     * 
     * @see http://en.wikipedia.org/wiki/Joule
     */
    double produce(double internalTemperature, long millis) {
        
        double internal = getProduction() * (millis / 1000);
        double leak = getAmbientLeak(internalTemperature, heatTolerance) * (millis / 1000);
        
        return  internal + leak;
    }
}
