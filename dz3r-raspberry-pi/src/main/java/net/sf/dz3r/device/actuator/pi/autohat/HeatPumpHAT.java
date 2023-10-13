package net.sf.dz3r.device.actuator.pi.autohat;

import com.homeclimatecontrol.autohat.pi.PimoroniAutomationHAT;
import net.sf.dz3r.counter.ResourceUsageCounter;
import net.sf.dz3r.device.actuator.HeatPump;
import net.sf.dz3r.model.HvacMode;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;

/**
 * Single stage heatpump driver based on Pimoroni Automation HAT.
 *
 * In addition to flipping relays, this implementation will use the {@code Power} and {@code Comms} lights
 * to indicate the fan and condenser status, respectively. {@code Warn} light is not used by this implementation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
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
    public HeatPumpHAT(String name, ResourceUsageCounter<Duration> uptimeCounter) throws IOException {
        this(name, AutomationHatWrapper.getInstance(), false, false, false, Duration.ZERO, uptimeCounter);
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
     * @param changeModeDelay Delay to observe while changing the {@link HvacMode operating mode}.
     * @param uptimeCounter Self-explanatory. Optional for now.
     */
    public HeatPumpHAT(
            String name,
            boolean reverseMode,
            boolean reverseRunning,
            boolean reverseFan,
            Duration changeModeDelay,
            ResourceUsageCounter<Duration> uptimeCounter) throws IOException {

        this(name, AutomationHatWrapper.getInstance(),
                reverseMode, reverseRunning, reverseFan,
                changeModeDelay,
                uptimeCounter);
    }

    private HeatPumpHAT(
            String name,
            AutomationHatWrapper hatWrapper,
            boolean reverseMode,
            boolean reverseRunning,
            boolean reverseFan,
            Duration changeModeDelay,
            ResourceUsageCounter<Duration> uptimeCounter) throws IOException {

        super(name,
                hatWrapper.relay().get(0), reverseMode,
                hatWrapper.relay().get(1), reverseRunning,
                hatWrapper.relay().get(2), reverseFan,
                changeModeDelay,
                uptimeCounter
        );

        setRelayLightsIntensity(relayLightsIntensity);
        setStatusLightsIntensity(statusLightsIntensity);
    }

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
    protected Mono<Boolean> setMode(boolean state) {

        var result = super.setMode(state);

        // VT: FIXME: Temporary solution, let's eat this elephant one bite at a time
        try {
            PimoroniAutomationHAT.getInstance().status().warn().intensity().write(statusLightsIntensity);
            PimoroniAutomationHAT.getInstance().status().warn().write(state);
            logger.warn("mode={} - unconfirmed, Mono returned", state);
        } catch (IOException ex) {
            logger.error("Error setting status lights, ignored", ex);
        }

        return result;
    }

    @Override
    protected Mono<Boolean> setRunning(boolean state) {
        var result = super.setRunning(state);

        // VT: FIXME: Temporary solution, let's eat this elephant one bite at a time
        try {
            PimoroniAutomationHAT.getInstance().status().comms().write(state);
            logger.debug("running={} - unconfirmed, Mono returned", state);
        } catch (IOException ex) {
            logger.error("Error setting status lights, ignored", ex);
        }

        return result;
    }

    @Override
    protected Mono<Boolean> setFan(boolean state) {
        var result = super.setFan(state);

        // VT: FIXME: Temporary solution, let's eat this elephant one bite at a time
        try {
            PimoroniAutomationHAT.getInstance().status().power().write(state);
            logger.debug("fan={} - unconfirmed, Mono returned", state);
        } catch (IOException ex) {
            logger.error("Error setting status lights, ignored", ex);
        }

        return result;
    }

    @Override
    protected void doClose() throws IOException {

        super.close();

        // Now, shut off the lights and don't spook the poor family
        PimoroniAutomationHAT.getInstance().status().warn().write(false);
        PimoroniAutomationHAT.getInstance().status().comms().write(false);
        PimoroniAutomationHAT.getInstance().status().power().write(false);

        for (var offset = 0; offset < 3; offset++) {
            var r = PimoroniAutomationHAT.getInstance().relay().get(offset);
            r.light().get(0).write(false);
            r.light().get(1).write(false);
        }

        logger.info("HAT: shut off the lights.");
    }
}
