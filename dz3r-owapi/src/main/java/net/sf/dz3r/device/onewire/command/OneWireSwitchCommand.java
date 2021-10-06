package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.utils.OWPath;
import net.sf.dz3r.device.onewire.OWPathResolver;
import reactor.core.publisher.FluxSink;

import java.util.UUID;

public abstract class OneWireSwitchCommand extends OneWireCommand {

    /**
     * Unlike sensors (where it is sufficient to just observe the status flux), switch commands may be received
     * when the path information is not yet available. Providing a resolver instance alleviates (but doesn't eliminate)
     * the problem.
     */
    public final OWPathResolver pathResolver;

    public final String address;

    /**
     * Device path.
     *
     * {@code null} means that the device is either not present, or hasn't been detected. Will have to deal with it downstream.
     */
    public final OWPath path;

    /**
     * Create an instance without the command sink.
     *
     * @param address Switch address to send the command to.
     * @param path Path the device is supposed to be on.
     */
    protected OneWireSwitchCommand(UUID messageId, FluxSink<OneWireCommand> commandSink, OWPathResolver pathResolver, String address, OWPath path) {
        super(messageId, commandSink);

        if (address == null) {
            throw new IllegalArgumentException("address can't be null");
        }

        if (pathResolver == null) {
            throw new IllegalArgumentException("pathResolver can't be null");
        }

        this.pathResolver = pathResolver;
        this.address = address;
        this.path = path;
    }
}
