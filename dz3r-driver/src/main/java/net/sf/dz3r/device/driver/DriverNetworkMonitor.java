package net.sf.dz3r.device.driver;

import net.sf.dz3r.device.driver.command.DriverCommand;
import reactor.core.publisher.FluxSink;

import java.io.IOException;

/**
 * Watches over a device driver network, handles events, executes commands.
 *
 * @param <T> Device adapter type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class DriverNetworkMonitor<T> {

    protected abstract T getAdapter() throws IOException;

    /**
     * Get the command sink.
     *
     * This method violates the Principle of Least Privilege (outsiders can do things with the sink they really shouldn't),
     * so let's make a mental note of replacing it with a complicated {@code submit()} that will shield the sink from
     * malicious (and dumb) programmers.
     *
     * @return The command sink.
     */
    public abstract FluxSink<DriverCommand<T>> getCommandSink();
}
