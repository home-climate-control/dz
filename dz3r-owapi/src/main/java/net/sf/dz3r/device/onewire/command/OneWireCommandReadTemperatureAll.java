package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import net.sf.dz3r.device.onewire.event.OneWireNetworkEvent;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;

/**
 * Command to read the temperatures from all {@link com.dalsemi.onewire.container.TemperatureContainer}
 * devices on the 1-Wire network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class OneWireCommandReadTemperatureAll extends OneWireCommand {

    public OneWireCommandReadTemperatureAll(FluxSink<OneWireCommand> commandSink) {
        super(commandSink);
    }

    @Override
    protected void execute(DSPortAdapter adapter, OneWireCommand command, FluxSink<OneWireNetworkEvent> eventSink) {
        ThreadContext.push("readTemperatureAll");
        try {
            logger.error("FIXME: readTemperatureAll()");
        } finally {
            ThreadContext.pop();
        }
    }
}
