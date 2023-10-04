package net.sf.dz3r.view.ha;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ZoneDiscoveryPacket {
    @JsonProperty("~")
    public final String rootTopic;
    public final String name;

    /**
     * Topic to receive "set mode" commands.
     */
    public final String modeCommandTopic = "~/mode/command";
    public final String modeStateTopic = "~/state";
    public final String modeStateTemplate = "{{value_json.mode}}";
    public final String[] modes;

    public final String actionTopic = "~/action/state";

    /**
     * Topic for sending "alive status".
     *
     * {@link #payloadAvailable} should be sent to indicate availability, {@link #payloadNotAvailable} should ideally be set as LWT.
     */
    public final String availabilityTopic = "~/status";
    public final String payloadAvailable = "online";
    public final String payloadNotAvailable = "offline";

    /**
     * Topic to receive "set temperature" commands.
     */
    public final String temperatureCommandTopic = "~/temp/command";
    public final String temperatureStateTopic = "~/state";
    public final String temperatureStateTemplate = "{{value_json.setpoint}}";
    public final String temperatureUnit = "C";
    public final String currentTemperatureTopic = "~/state";
    public final String currentTemperatureTemplate = "{{value_json.current_temperature}}";
    public final double minTemp;
    public final double maxTemp;
    public final double tempStep = 0.1;

    public final String uniqueId;

    public final DeviceDiscoveryPacket device;

    public ZoneDiscoveryPacket(String rootTopic, String name, String[] modes, double minTemp, double maxTemp, String uniqueId, DeviceDiscoveryPacket device) {
        this.rootTopic = rootTopic;
        this.name = name;
        this.modes = modes;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.uniqueId = uniqueId;
        this.device = device;
    }
}
