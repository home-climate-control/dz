package net.sf.dz3r.view.ha;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ZoneDiscoveryPacket {
    @JsonProperty("~")
    public final String rootTopic;
    public final String name;

    public final String modeCommandTopic = "~/mode/command";
    public final String modeStateTopic = "~/mode/state";
    public final String modeStateTemplate = "{{value_json.mode}}";
    public final String[] modes;

    public final String actionTopic = "~/action/state";
    public final String availabilityTopic = "~/status";
    public final String payloadAvailable = "online";
    public final String payloadNotAvailable = "offline";

    public final String temperatureCommandTopic = "~/temp";
    public final String temperatureStateTopic = "~/state";
    public final String temperatureStateTemplate = "{{value_json.target_temp}}";
    public final String temperatureUnit = "C";
    public final String currentTemperatureTopic = "~/state";
    public final String currentTemperatureTemplate = "{{value_json.current_temp}}";
    public final int minTemp;
    public final int maxTemp;
    public final double tempStep = 0.1;

    public final String uniqueId;

    public final DeviceDiscoveryPacket device;

    public ZoneDiscoveryPacket(String rootTopic, String name, String[] modes, int minTemp, int maxTemp, String uniqueId, DeviceDiscoveryPacket device) {
        this.rootTopic = rootTopic;
        this.name = name;
        this.modes = modes;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.uniqueId = uniqueId;
        this.device = device;
    }
}
