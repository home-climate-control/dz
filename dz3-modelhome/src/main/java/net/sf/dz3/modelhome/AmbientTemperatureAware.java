package net.sf.dz3.modelhome;

/**
 * An entity aware of ambient temperature and heat transfer from inside out.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2012
 */
public class AmbientTemperatureAware {

    /**
     * Ambient temperature, in °C.
     */
    private double ambientTemperature;

    /**
     * @param ambientTemperature Initial ambient temperature value, in °C.
     */
    public AmbientTemperatureAware(double ambientTemperature) {
        setAmbientTemperature(ambientTemperature);
    }

    /**
     * Make this object aware of a change in the ambient temperature.
     * No recalculation of internal state is happening here.
     *  
     * @param ambientTemperature New ambient temperature value, in °C.
     */
    public final void setAmbientTemperature(double ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }
    
    /**
     * @return {@link #ambientTemperature}, in °C.
     */
    public final double getAmbientTemperature() {
        return ambientTemperature;
    }

    /**
     * @param internalTemperature Internal temperature of the object, in °C.
     * @param insulationConductivity Thermal conductivity of the object's insulation
     * 
     * @return Flow rate at which energy leaks from outside in, in watts (joules per second).
     * 
     * @see http://en.wikipedia.org/wiki/Heat_equation
     * @see http://en.wikipedia.org/wiki/Watt
     */
    protected final double getAmbientLeak(double internalTemperature, double insulationConductivity) {
        
        // Simple one-dimensional approximation
        return ((getAmbientTemperature() - internalTemperature) * insulationConductivity);
    }
}
