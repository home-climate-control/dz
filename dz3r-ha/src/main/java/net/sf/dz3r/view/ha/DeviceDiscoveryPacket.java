package net.sf.dz3r.view.ha;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceDiscoveryPacket {

    public final String identifiers;
    public final String model;
    public final String name;
    public final String swVersion;
    public final String manufacturer;

    public DeviceDiscoveryPacket(String identifiers, String model, String name, String swVersion, String manufacturer) {

        this.identifiers = identifiers;
        this.model = model;
        this.name = name;
        this.swVersion = swVersion;
        this.manufacturer = manufacturer;
    }
}
