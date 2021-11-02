package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import net.sf.dz3r.device.driver.command.DriverCommand;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all 1-Wire commands.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class OneWireCommand extends DriverCommand<DSPortAdapter> {

    protected OneWireCommand(UUID messageId, FluxSink<DriverCommand<DSPortAdapter>> commandSink) {
        super(messageId, commandSink);
    }

    @Override
    protected List<Boolean> assessErrorFlags(Exception ex) {

        if (ex.getMessage() != null && ex.getMessage().equals("Error short on 1-Wire during putByte")) {
            return List.of(
                    true, // transient, can be corrected on the fly
                    false // not fatal, even though it kills the network, the network comes back after the short is gone
            );
        }

        var result = new ArrayList<Boolean>();

        result.add(null);
        result.add(null);

        return result;
    }

    @Override
    public String toString() {
        return "{" + getClass().getSimpleName() + " messageId=" + messageId + "}";
    }
}
