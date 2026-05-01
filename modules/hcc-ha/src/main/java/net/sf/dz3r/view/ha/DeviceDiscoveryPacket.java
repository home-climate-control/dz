package net.sf.dz3r.view.ha;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DeviceDiscoveryPacket(
        String identifiers,
        String model,
        String name,
        String swVersion,
        String manufacturer) {

}
