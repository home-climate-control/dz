package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;

/**
 * Command to read the temperatures from all {@link com.dalsemi.onewire.container.TemperatureContainer}
 * devices on the 1-Wire network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class OneWireCommandReadTemperatureAll extends OneWireCommand {
    @Override
    public Flux<OneWireNetworkEvent<?>> execute(DSPortAdapter adapter, OneWireCommand command) {
        ThreadContext.push("readTemperatureAll");
        try {
            logger.error("FIXME: readTemperatureAll()");
            return Flux.empty();
        } finally {
            ThreadContext.pop();
        }
    }
}
