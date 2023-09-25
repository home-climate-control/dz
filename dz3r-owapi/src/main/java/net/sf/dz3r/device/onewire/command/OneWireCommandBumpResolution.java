package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.TemperatureContainer;
import com.dalsemi.onewire.utils.OWPath;
import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;

import java.util.UUID;

public class OneWireCommandBumpResolution extends OneWireCommand {

    public final String address;
    public final OWPath path;

    public OneWireCommandBumpResolution(FluxSink<DriverCommand<DSPortAdapter>> commandSink, String address, OWPath path) {
        super(UUID.randomUUID(), commandSink);
        this.address = address;
        this.path = path;
    }

    @Override
    protected void execute(DSPortAdapter adapter, DriverCommand<DSPortAdapter> command, FluxSink<DriverNetworkEvent> eventSink) throws Exception {
        ThreadContext.push("bumpResolution");
        var m = new Marker("bumpResolution");
        try {

            var device = adapter.getDeviceContainer(address);
            adapter.closeAllPaths();
            path.open();

            if (!(device instanceof TemperatureContainer temperatureContainer)) {
                logger.debug("{} ({}): not a temperature container", address, device.getName());
                return;
            }

            var state = temperatureContainer.readDevice();

            if (!temperatureContainer.hasSelectableTemperatureResolution()) {
                logger.debug("{} ({}): doesn't support selectable resolution", address, device.getName());
                return;
            }

            var resolutions = temperatureContainer.getTemperatureResolutions();
            var sb = new StringBuilder();

            for (double v : resolutions) {
                sb.append(v).append(" ");
            }

            logger.debug("{} ({}): temperature resolutions available: {}, setting best", address, device.getName(), sb);

            temperatureContainer.setTemperatureResolution(resolutions[resolutions.length - 1], state);
            temperatureContainer.writeDevice(state);

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }
}
