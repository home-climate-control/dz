package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.HvacDeviceStatus;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.util.Set;

public class HeatPump extends AbstractHvacDevice {

    private final Switch switchMode;
    private final Switch switchRunning;
    private final Switch switchFan;

    private final boolean reverseMode;
    private final boolean reverseRunning;
    private final boolean reverseFan;

    /**
     * Create an instance with all straight switches.
     *
     * @param name JMX name.
     * @param switchMode Switch to pull to change the operating mode.
     * @param switchRunning Switch to pull to turn on the compressor.
     * @param switchFan Switch to pull to turn on the air handler.
     */
    public HeatPump(String name, Switch switchMode, Switch switchRunning, Switch switchFan) {
        this(name, switchMode, false, switchRunning, false, switchFan, false);
    }

    /**
     * Create an instance with some switches possibly reverse.
     *
     * @param name JMX name.
     * @param switchMode Switch to pull to change the operating mode.
     * @param reverseMode {@code true} if the "off" mode position corresponds to logical one.
     * @param switchRunning Switch to pull to turn on the compressor.
     * @param reverseRunning {@code true} if the "off" running position corresponds to logical one.
     * @param switchFan Switch to pull to turn on the air handler.
     * @param reverseFan {@code true} if the "off" fan position corresponds to logical one.
     */
    protected HeatPump(
            String name,
            Switch switchMode, boolean reverseMode,
            Switch switchRunning, boolean reverseRunning,
            Switch switchFan, boolean reverseFan) {

        super(name);

        check(switchMode, "mode");
        check(switchRunning, "running");
        check(switchFan, "fan");

        this.switchMode = switchMode;
        this.switchRunning = switchRunning;
        this.switchFan = switchFan;

        this.reverseMode = reverseMode;
        this.reverseRunning = reverseRunning;
        this.reverseFan = reverseFan;
    }

    @Override
    public Set<HvacMode> getModes() {
        return Set.of(HvacMode.COOLING, HvacMode.HEATING);
    }

    @Override
    public Flux<Signal<HvacDeviceStatus, Void>> compute(Flux<Signal<HvacCommand, Void>> in) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Single Stage Heatpump Driver (energize to heat)",
                getAddress(),
                "Controls single stage heat pump");
    }
}
