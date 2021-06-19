package net.sf.dz3.modelhome;

/**
 * A room full of air.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2012
 */
public class Room extends AmbientTemperatureAware {

    /**
     * Area, in square meters.
     */
    private double area;

    /**
     * Ceiling height, in meters.
     */
    private double ceiling;

    /**
     * Specific heat capacity of air, in joule per gram kelvin.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Heat_capacity">Heat capacity</a>
     */
    private final double specificHeatCapacity = 1.012;

    /**
     * Air density at 25°C (good enough for our purposes), g/m^3.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Density_of_air">Density of air</a>
     */
    private final double airDensity = 1184;

    /**
     * Thermal conductivity of the insulation, units to be defined.
     */
    private double insulationConductivity;

    /**
     * Quality of supply, 1 being unrestricted, 0 being totally clogged up.
     */
    private double supplyQuality;

    /**
     * Room internal temperature, in °C.
     */
    private double internalTemperature;

    /**
     * Create an instance.
     *
     * @param area Room area, in square meters.
     * @param ceiling Ceiling height, in meters.
     * @param insulationConductivity Insulation conductivity, units to be defined.
     * @param supplyQuality Air supply quality, 0 (totally clogged up) to 1 (unrestricted).
     * @param internalTemperature Initial internal temperature, in °C.
     * @param ambientTemperature Initial ambient temperature, in °C.
     */
    public Room(double area, double ceiling,
            double insulationConductivity, double supplyQuality,
            double internalTemperature, double ambientTemperature) {

        super(ambientTemperature);

        setArea(area);
        setCeiling(ceiling);
        setInsulationConductivity(insulationConductivity);
        setSupplyQuality(supplyQuality);
        setInternalTemperature(internalTemperature);
    }

    /**
     * Set a new value for {@link #area}.
     *
     * @param area New value for {@link #area}, in square meters.
     */
    public void setArea(double area) {

        if (area <= 0) {
            throw new IllegalArgumentException("Area must be positive");
        }

        this.area = area;
    }

    /**
     * Set a new value for {@link #ceiling}.
     *
     * @param ceiling New value for {@link #ceiling}, in meters.
     */
    public void setCeiling(double ceiling) {

        if (ceiling <= 0) {
            throw new IllegalArgumentException("Ceiling must be positive");
        }

        this.ceiling = ceiling;
    }

    /**
     * Set a new value for {@link #insulationConductivity}.
     *
     * @param conductivity New value for {@link #insulationConductivity}.
     */
    public void setInsulationConductivity(double conductivity) {

        // VT: FIXME: Disabled until units are figured out
//        if (conductivity < 0 || conductivity > 1) {
//            throw new IllegalArgumentException("Valid values are 0..1");
//        }

        this.insulationConductivity = conductivity;
    }

    /**
     * Set a new value for {@link #supplyQuality}.
     *
     * @param quality New value for {@link #supplyQuality}.
     */
    public void setSupplyQuality(double quality) {

        if (quality < 0 || quality > 1) {
            throw new IllegalArgumentException("Valid values are 0..1");
        }

        this.supplyQuality = quality;
    }

    /**
     * Forcibly set the {@link #internalTemperature} to a new value.
     * Normally needed only at startup.
     *
     * @param temperature Temperature to set, in °C.
     */
    public void setInternalTemperature(double temperature) {
        this.internalTemperature = temperature;
    }

    /**
     * @return {@link #internalTemperature}, in °C.
     */
    public final double getInternalTemperature() {
        return internalTemperature;
    }

    /**
     * @return Room volume, in cubic meters.
     */
    private double getVolume() {
        return area * ceiling;
    }

    /**
     * @return Total heat capacity of the room, in joules per kelvin.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Heat_capacity">Heat capacity</a>
     * @see <a href="https://en.wikipedia.org/wiki/Joule">Joule</a>
     * @see <a href="https://en.wikipedia.org/wiki/Kelvin</a>
     */
    private double getHeatCapacity() {
        return getVolume() * airDensity * specificHeatCapacity;
    }

    /**
     * Calculate change in temperature after consuming some energy per time interval,
     * taking ambient leak into account.
     *
     * @param energy Amount of energy consumed, in joules. Positive for heating, negative for cooling.
     * @param millis Time interval for the amount of energy being consumed, in milliseconds.
     *
     * @return New {@link #internalTemperature}, in °C.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Joule">Joule</a>
     */
    public synchronized double consume(double energy, long millis) {

        double ambientLeak = getAmbientLeak(internalTemperature, insulationConductivity) * (millis / 1000d);
        double deltaT = ((energy * supplyQuality) + ambientLeak) / getHeatCapacity();

        internalTemperature += deltaT;

        return internalTemperature;
    }
}
