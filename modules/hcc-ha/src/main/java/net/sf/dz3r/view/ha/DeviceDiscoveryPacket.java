package net.sf.dz3r.view.ha;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DeviceDiscoveryPacket(
        String identifiers,
        String model,
        String name,
        String swVersion,
        String manufacturer) {

}
