package net.sf.dz3.device.sensor;

/**
 * Sensor type definitions.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 */
public enum SensorType {

    GENERIC("G", "generic"),
    TEMPERATURE("T", "temperature"),
    HUMIDITY("H", "humidity"),
    PRESSURE("P", "pressure"),
    PROTOTYPE("@", "prototype"),
    SWITCH("S", "switch");

    public final String type;
    public final String description;

    private SensorType(String type, String description) {

        this.type = type;
        this.description = description;
    }

    @Override
    public String toString() {

        return type;
    }
}
