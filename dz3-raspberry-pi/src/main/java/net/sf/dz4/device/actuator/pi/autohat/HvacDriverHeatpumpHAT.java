package net.sf.dz4.device.actuator.pi.autohat;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.actuator.impl.HvacDriverHeatpump;

import java.io.IOException;

/**
 * Single stage heatpump driver based on Pimoroni Automation HAT.
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
     * @param hatWrapper Automation HAT wrapper instance. Might not be needed later.
     * @param reverseMode {@code true} if the "off" mode position corresponds to logical one.
     * @param reverseRunning {@code true} if the "off" running position corresponds to logical one.
     * @param reverseFan {@code true} if the "off" fan position corresponds to logical one.
     */
    public HvacDriverHeatpumpHAT(
            AutomationHatWrapper hatWrapper,
            boolean reverseMode,
            boolean reverseRunning,
            boolean reverseFan) {

        super(
                hatWrapper.relay().get(0), reverseMode,
                hatWrapper.relay().get(0), reverseRunning,
                hatWrapper.relay().get(0), reverseFan
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
}
