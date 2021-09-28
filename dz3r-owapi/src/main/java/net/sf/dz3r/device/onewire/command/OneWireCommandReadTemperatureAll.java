package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.Command;
import com.dalsemi.onewire.container.TemperatureContainer;
import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3r.device.onewire.event.OneWireNetworkEvent;
import net.sf.dz3r.device.onewire.event.OneWireNetworkTemperatureSample;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;

import java.time.Instant;
import java.util.Set;

/**
 * Command to read the temperatures from all {@link com.dalsemi.onewire.container.TemperatureContainer}
 * devices on the 1-Wire network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class OneWireCommandReadTemperatureAll extends OneWireCommand {
    public final Set<String> knownDevices;

    public OneWireCommandReadTemperatureAll(FluxSink<OneWireCommand> commandSink, Set<String> knownDevices) {
        super(commandSink);
        this.knownDevices = knownDevices;
    }

    @Override
    protected void execute(DSPortAdapter adapter, OneWireCommand command, FluxSink<OneWireNetworkEvent> eventSink) throws OneWireException {
        ThreadContext.push("readTemperatureAll");
        var m = new Marker("readTemperatureAll");
        try {

            // Brutal 1-Wire network hack: select all devices at the same time, issue convert command,
            // then read them one by one.

            adapter.reset();
            adapter.putByte(Command.SELECT_ALL.code);
            adapter.putByte(Command.CONVERT_TEMPERATURE.code);

            try {
                Thread.sleep(750);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted, ignored", ex);
            }

            // VT: FIXME: This can be improved by passing down device containers so it can be immediately determined
            //  which are the temperature containers

            for (var address : knownDevices) {
                readTemperature(adapter, address, eventSink);
            }

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void readTemperature(DSPortAdapter adapter, String address, FluxSink<OneWireNetworkEvent> eventSink) throws OneWireException {

        ThreadContext.push("readTemperature");
        try {

            var owc = adapter.getDeviceContainer(address);

            if (!(owc instanceof TemperatureContainer)) {
                logger.debug("not a temperature container: {}", address);
                return;
            }

            var tc = (TemperatureContainer) owc;
            var sample = tc.getTemperature(tc.readDevice());

            logger.info("{}: {}°C", address, sample);

            if (Double.compare(sample, 85.0) == 0) {
                // This is actually pretty serious, better pay attention
                logger.error("{}: 85°C, ignored", address);

                // Got the reading, but it's bad, no event emitted
                return;
            }

            eventSink.next(new OneWireNetworkTemperatureSample(Instant.now(), address, sample));

        } finally {
            ThreadContext.pop();
        }
    }
}
