package net.sf.dz4.device.actuator.pi.autohat;

import com.homeclimatecontrol.autohat.pi.PimoroniAutomationHAT;
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
            boolean reverseFan) {

        super(
                hatWrapper.relay().get(0), reverseMode,
                hatWrapper.relay().get(1), reverseRunning,
                hatWrapper.relay().get(2), reverseFan
        );
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Single Stage Heatpump Driver (energize to heat)",
                Integer.toHexString(hashCode()),
                "Controls single stage heat pump connected to Pimoroni Automation HAT");
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
