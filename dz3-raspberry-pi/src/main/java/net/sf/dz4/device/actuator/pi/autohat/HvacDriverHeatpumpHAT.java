package net.sf.dz4.device.actuator.pi.autohat;

import com.homeclimatecontrol.autohat.pi.PimoroniAutomationHAT;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.actuator.impl.HvacDriverHeatpump;

import java.io.IOException;

/**
 * Single stage heatpump driver based on Pimoroni Automation HAT.
 *
 * In addition to flipping relays, this implementation will use the {@code Power} and {@code Comms} lights
 * to indicate the fan and condenser status, respectively. {@code Warn} light is not used by this implementation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class HvacDriverHeatpumpHAT extends HvacDriverHeatpump {

    /**
     * Default relay light intensity (approximately {@code 0x11}).
     *
     * These lights are small and are next to each other, making them brighter will not make them
     * more visible.
     */
    private double relayLightsIntensity = 0.066;

    /**
     * Default status light intensity (approximately {@code 0x22}).
     */
    private double statusLightsIntensity = 0.13;

    /**
     * Create an instance with all straight switches.
     */
    public HvacDriverHeatpumpHAT() throws IOException {
        this(AutomationHatWrapper.getInstance(), false, false, false);
    }

    /**
     * Create an instance with some switches possibly reverse.
     *
     * You probably don't want to use this constructor, since the HAT offers both NO and NC contacts, it would be
     * better to just use the other pair.
     *
     * @param reverseMode {@code true} if the "off" mode position corresponds to logical one.
     * @param reverseRunning {@code true} if the "off" running position corresponds to logical one.
     * @param reverseFan {@code true} if the "off" fan position corresponds to logical one.
     */
    public HvacDriverHeatpumpHAT(
            boolean reverseMode,
            boolean reverseRunning,
            boolean reverseFan) throws IOException {

        this(AutomationHatWrapper.getInstance(), reverseMode, reverseRunning, reverseFan);
    }

    private HvacDriverHeatpumpHAT(
            AutomationHatWrapper hatWrapper,
            boolean reverseMode,
            boolean reverseRunning,
            boolean reverseFan) throws IOException {

        super(
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
                Integer.toHexString(hashCode()),
                "Controls single stage heat pump connected to Pimoroni Automation HAT");
    }

    @JmxAttribute(description = "Relay lights intensity. 1.0 is VERY bright.")
    public double getRelayLightIntensity() {
        return relayLightsIntensity;
    }

    public void setRelayLightsIntensity(double relayLightsIntensity) throws IOException {
        if (relayLightsIntensity < 0 || relayLightsIntensity > 1) {
            throw new IllegalArgumentException("intensity value should be in 0..1 range (" + relayLightsIntensity + " given)");
        }

        for (int offset = 0; offset < 3; offset++) {
            var r = PimoroniAutomationHAT.getInstance().relay().get(offset);
            r.light().get(0).intensity().write(relayLightsIntensity);
            r.light().get(1).intensity().write(relayLightsIntensity);
        }

        this.relayLightsIntensity = relayLightsIntensity;
    }

    @JmxAttribute(description = "Status lights intensity. 1.0 is VERY bright.")
    public double getStatusLightIntensity() {
        return statusLightsIntensity;
    }

    public void setStatusLightsIntensity(double statusLightsIntensity) throws IOException {
        if (statusLightsIntensity < 0 || statusLightsIntensity > 1) {
            throw new IllegalArgumentException("intensity value should be in 0..1 range (" + statusLightsIntensity + " given)");
        }

        var hat = PimoroniAutomationHAT.getInstance();

        // We just control Power and Comms, but not Warn - that's somebody else's business
        hat.status().power().intensity().write(statusLightsIntensity);
        hat.status().comms().intensity().write(statusLightsIntensity);

        this.statusLightsIntensity = statusLightsIntensity;
    }

    /**
     * Do what the superclass does, and flip the {@code Comms} (blue) light on when on.
     *
     * @param stage Stage to set.
     *
     * @throws IOException If there was a problem communicating with hardware.
     */
    @Override
    protected synchronized void doSetStage(int stage) throws IOException {
        super.doSetStage(stage);
        PimoroniAutomationHAT.getInstance().status().comms().write(stage > 0);
    }

    /**
     * Do what the superclass does, and flip the {@code Power} (green) light on when on.
     *
     * @param speed Fan speed to set.
     *
     * @throws IOException If there was a problem communicating with hardware.
     */
    @Override
    protected synchronized void doSetFanSpeed(double speed) throws IOException {
        super.doSetFanSpeed(speed);
        PimoroniAutomationHAT.getInstance().status().power().write(speed > 0);
    }
}
