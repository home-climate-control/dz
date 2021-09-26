package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import net.sf.dz3r.device.onewire.event.OneWireNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Base class for all 1-Wire commands.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class OneWireCommand {
    protected final Logger logger = LogManager.getLogger();

    /**
     * Sink to use to issue more commands if necessary.
     */
    protected final FluxSink<OneWireCommand> commandSink;

    protected OneWireCommand(FluxSink<OneWireCommand> commandSink) {
        this.commandSink = commandSink;
    }

    public final Flux<OneWireNetworkEvent> execute(DSPortAdapter adapter, OneWireCommand command) {
        return Flux.create(sink -> {
            execute(adapter, command, sink);
            sink.complete();
        });
    }

    protected abstract void execute(DSPortAdapter adapter, OneWireCommand command, FluxSink<OneWireNetworkEvent> eventSink);

    /**
     * Close all open device paths.
     *
     * This is a shortcut using DS2409 specific hardware commands to close all open paths.
     * Must be modified if a different adapter is ever used.
     *
     * @throws OneWireException if anything goes wrong.
     */
    protected void closeAllPaths(DSPortAdapter adapter) throws OneWireException {

        // DS2409 specific - skip, all lines off

        adapter.reset();
        adapter.putByte(0x00CC);
        adapter.putByte(0x0066);
        adapter.getByte();
    }

}
