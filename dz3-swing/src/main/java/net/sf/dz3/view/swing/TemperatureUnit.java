package net.sf.dz3.view.swing;

public enum TemperatureUnit {

    CELSIUS("Celsius", "C"),
    FAHRENHEIT("Fahrenheit", "F");

    public final String longName;
    public final String shortName;

    private TemperatureUnit(String longName, String shortName) {
        this.longName = longName;
        this.shortName = shortName;
    }

    public static TemperatureUnit resolve(String source) {

        try {

            return valueOf(source);

        } catch (Throwable t) {

            if (source.toUpperCase().startsWith("C")) {
                return CELSIUS;
            }

            if (source.toUpperCase().startsWith("F")) {
                return FAHRENHEIT;
            }

            throw new IllegalArgumentException("need either Celsius or Fahrenheit here, got '" + source + "' instead)");

        }
    }
}
