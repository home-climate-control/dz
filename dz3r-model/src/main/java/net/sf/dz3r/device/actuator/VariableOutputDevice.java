package net.sf.dz3r.device.actuator;

import static net.sf.dz3r.device.actuator.VariableOutputDevice.Command;
import static net.sf.dz3r.device.actuator.VariableOutputDevice.OutputState;

/**
 * Variable output device.
 *
 * This specification honors <a href="https://martinfowler.com/bliki/CQRS.html">Command Query Responsibility Segregation</a>.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface VariableOutputDevice extends CqrsDevice<Command, OutputState> {

    /**
     * Command to pass to the device.
     *
     * @param on Whether to turn the device on or off.
     * @param output Output power, {@code 0.0} to {@code 1.0} inclusive.
     */
    record Command(
            boolean on,
            double output
    ) {

    }

    record OutputState(
            Boolean on,
            Double output
    ) {

    }
}
