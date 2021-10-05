package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.TemperatureContainer;
import com.dalsemi.onewire.utils.OWPath;
import net.sf.dz3r.device.onewire.event.OneWireNetworkEvent;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;

import java.util.UUID;

public class OneWireCommandBumpResolution extends OneWireCommand {

    public final String address;
    public final OWPath path;

    public OneWireCommandBumpResolution(FluxSink<OneWireCommand> commandSink, String address, OWPath path) {
        super(UUID.randomUUID(), commandSink);
        this.address = address;
        this.path = path;
    }

    @Override
    protected void execute(DSPortAdapter adapter, OneWireCommand command, FluxSink<OneWireNetworkEvent> eventSink) throws OneWireException {
        ThreadContext.push("bumpResolution");
        var m = new Marker("bumpResolution");
        try {

            var device = adapter.getDeviceContainer(address);
            adapter.closeAllPaths();
            path.open();

            if (!(device instanceof TemperatureContainer)) {
                logger.debug("{} ({}): not a temperature container", address, device.getName());
                return;
            }

            var tc = (TemperatureContainer) device;

            var state = tc.readDevice();

            if (!tc.hasSelectableTemperatureResolution()) {
                logger.debug("{} ({}): doesn't support selectable resolution", address, device.getName());
                return;
            }

            var resolutions = tc.getTemperatureResolutions();
            var sb = new StringBuilder();

            for (double v : resolutions) {
                sb.append(v).append(" ");
            }

            logger.debug("{} ({}): temperature resolutions available: {}, setting best", address, device.getName(), sb);

            tc.setTemperatureResolution(resolutions[resolutions.length - 1], state);
            tc.writeDevice(state);

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }
}
