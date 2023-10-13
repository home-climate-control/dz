package net.sf.dz3r.device.xbee.command;

import com.homeclimatecontrol.xbee.XBeeReactive;
import net.sf.dz3r.device.driver.command.DriverCommand;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all XBee commands.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class XBeeCommand extends DriverCommand<XBeeReactive> {

    protected XBeeCommand(UUID messageId, FluxSink<DriverCommand<XBeeReactive>> commandSink) {
        super(messageId, commandSink);
    }

    @Override
    protected List<Boolean> assessErrorFlags(Exception ex) {

        // For now, return "don't know", fill in the details later

        var result = new ArrayList<Boolean>();

        result.add(null);
        result.add(null);

        return result;
    }
}
