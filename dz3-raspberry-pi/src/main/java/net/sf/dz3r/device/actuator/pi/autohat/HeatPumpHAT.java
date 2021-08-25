package net.sf.dz3r.device.actuator.pi.autohat;

import com.homeclimatecontrol.autohat.pi.PimoroniAutomationHAT;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3r.device.actuator.HeatPump;
import net.sf.dz4.device.actuator.pi.autohat.AutomationHatWrapper;

import java.io.IOException;

/**
 * Single stage heatpump driver based on Pimoroni Automation HAT.
 *
 * In addition to flipping relays, this implementation will use the {@code Power} and {@code Comms} lights
 * to indicate the fan and condenser status, respectively. {@code Warn} light is not used by this implementation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class HeatPumpHAT extends HeatPump {

    /**
     * Default relay light intensity (minimum).
     *
     * These lights are small and are next to each other, making them brighter will not make them
     * more visible.
     */
    private byte relayLightsIntensity = 1;

    /**
     * Default status light intensity (minimum).
     */
    private byte statusLightsIntensity = 1;

    /**
     * Create an instance with all straight switches.
     *
     * @param name JMX name.
     */
    public HeatPumpHAT(String name) throws IOException {
        this(name, AutomationHatWrapper.getInstance(), false, false, false);
    }

    /**
     * Create an instance with some switches possibly reverse.
     *
     * You probably don't want to use this constructor, since the HAT offers both NO and NC contacts, it would be
     * better to just use the other pair.
     *
     * @param name JMX name.
     * @param reverseMode {@code true} if the "off" mode position corresponds to logical one.
     * @param reverseRunning {@code true} if the "off" running position corresponds to logical one.
     * @param reverseFan {@code true} if the "off" fan position corresponds to logical one.
     */
    public HeatPumpHAT(
            String name,
            boolean reverseMode,
            boolean reverseRunning,
            boolean reverseFan) throws IOException {

        this(name, AutomationHatWrapper.getInstance(), reverseMode, reverseRunning, reverseFan);
    }

    private HeatPumpHAT(
            String name,
            AutomationHatWrapper hatWrapper,
            boolean reverseMode,
            boolean reverseRunning,
            boolean reverseFan) throws IOException {

        super(name,
                hatWrapper.relay().get(0), reverseMode,
                hatWrapper.relay().get(1), reverseRunning,
                hatWrapper.relay().get(2), reverseFan
        );

        setRelayLightsIntensity(relayLightsIntensity);
        setStatusLightsIntensity(statusLightsIntensity);
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Single Stage Heatpump Driver (energize to heat)",
                getAddress(),
                "Controls single stage heat pump connected to Pimoroni Automation HAT");
    }

    @JmxAttribute(description = "Relay lights intensity. 1.0 is VERY bright.")
    public double getRelayLightsIntensity() {
        return relayLightsIntensity;
    }

    public void setRelayLightsIntensity(byte relayLightsIntensity) throws IOException {
        if (relayLightsIntensity < 0 || relayLightsIntensity > 1) {
            throw new IllegalArgumentException("intensity value should be in 0..1 range (" + relayLightsIntensity + " given)");
        }

        for (var offset = 0; offset < 3; offset++) {
            var r = PimoroniAutomationHAT.getInstance().relay().get(offset);
            r.light().get(0).intensity().write(relayLightsIntensity);
            r.light().get(1).intensity().write(relayLightsIntensity);
        }

        this.relayLightsIntensity = relayLightsIntensity;
    }

    @JmxAttribute(description = "Status lights intensity. 1.0 is VERY bright.")
    public double getStatusLightsIntensity() {
        return statusLightsIntensity;
    }

    public void setStatusLightsIntensity(byte statusLightsIntensity) throws IOException {

        var hat = PimoroniAutomationHAT.getInstance();

        // We just control Power and Comms, but not Warn - that's somebody else's business
        hat.status().power().intensity().write(statusLightsIntensity);
        hat.status().comms().intensity().write(statusLightsIntensity);

        this.statusLightsIntensity = statusLightsIntensity;
    }

    @Override
    protected void setMode(boolean state) throws IOException {
        super.setMode(state);
        PimoroniAutomationHAT.getInstance().status().warn().write(state);
        logger.debug("mode={}", state);
    }

    @Override
    protected void setRunning(boolean state) throws IOException {
        super.setRunning(state);
        PimoroniAutomationHAT.getInstance().status().comms().write(state);
        logger.debug("running={}", state);
    }

    @Override
    protected void setFan(boolean state) throws IOException {
        super.setFan(state);
        PimoroniAutomationHAT.getInstance().status().power().write(state);
        logger.debug("fan={}", state);
    }
}
