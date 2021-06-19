package net.sf.dz3.device.actuator;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.impl.HvacDriverSignal;

import java.io.IOException;

/**
 * The actual HVAC unit driver.
 *
 * <p>
 *
 * The class implementing this interface controls the hardware.
 *
 * No sanity checks are required, they are provided by the {@link HvacController}.
 *
 * Same goes for timings - the operations performed
 * by this class may have to be as slow as it takes, it is irrelevant.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public interface HvacDriver extends JmxAware {

    /**
     * @return [requested, actual] operating mode.
     */
    @JmxAttribute(description = "Operating mode")
    HvacMode[] getMode();

    /**
     * @return [requested, actual] operating stage.
     */
    @JmxAttribute(description = "Stage")
    int[] getStage();

    /**
     * @return [requested, actual] fan speed.
     */
    @JmxAttribute(description = "Fan speed")
    double[] getFanSpeed();

    /**
     * Set the unit mode.
     *
     * @param mode Operating mode.
     *
     * Actually, the switch is only between cooling and heating, but
     * different units may have different rules (the contact is energized
     * only for cooling, or only for heating).
     *
     * @exception IOException if there was a problem talking to the hardware.
     */
    void setMode(HvacMode mode) throws IOException;

    /**
     * Set the unit stage.
     *
     * For both cooling and heating, the stage is always positive.
     *
     * @param stage A/C stage. For single stage units, 0 is off, 1 is on.
     * For multistage units, it is 0, 1, 2 and so on.
     *
     * @exception IOException if there was a problem talking to the hardware.
     */
    void setStage(int stage) throws IOException;

    /**
     * Set the fan speed.
     *
     * @param speed Fan speed. For single speed fan, 0 is off, 1 is on. For
     * variable speed or multispeed fans, 0 is off, 1 is full, anything in
     * between defines the actual speed. Since both variable speed and
     * multispeed fans have some preset values, it is up to the
     * implementation to define the rounding rules.
     *
     * @exception IOException if there was a problem talking to the hardware.
     */
    void setFanSpeed(double speed) throws IOException;

    /**
     * Power off the device, synchronously.
     *
     * Alert the user if things go really wrong, can't afford to let the exception through.
     */
    void powerOff();

    /**
     * Get current driver signal.
     *
     * @return Current driver signal.
     */
    @JmxAttribute(description = "Driver signal")
    HvacDriverSignal getSignal();
}
