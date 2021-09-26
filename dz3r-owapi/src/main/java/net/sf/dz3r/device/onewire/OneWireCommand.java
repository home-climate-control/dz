package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

/**
 * Base class for all 1-Wire commands.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class OneWireCommand {
    protected final Logger logger = LogManager.getLogger();
    public abstract Flux<OneWireNetworkEvent<?>> execute(DSPortAdapter adapter, OneWireCommand command);
}
