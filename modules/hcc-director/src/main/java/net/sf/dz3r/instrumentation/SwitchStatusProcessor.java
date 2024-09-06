package net.sf.dz3r.instrumentation;

import com.homeclimatecontrol.hcc.device.DeviceState;
import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.health.SwitchStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * Consumes individual switch signal, emits switch status.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public class SwitchStatusProcessor implements SignalProcessor<DeviceState<Boolean>, SwitchStatus, String> {

    private final Logger logger = LogManager.getLogger();

    private final String id;

    public SwitchStatusProcessor(String id) {
        this.id = id;

        logger.info("created switch status processor for id={}", id);
    }

    @Override
    public Flux<Signal<SwitchStatus, String>> compute(Flux<Signal<DeviceState<Boolean>, String>> in) {
        return in.map(this::compute);
    }

    private Signal<SwitchStatus, String> compute(Signal<DeviceState<Boolean>, String> source) {

        if (source.isError()) {
            // Nothing else matters, for now
            return new Signal<>(source.timestamp(), null, null, source.status(), source.error());
        }

        // VT: FIXME: Pass/fail is the only thing of interest right now, but DeviceState contains some juicy bits
        return new Signal<>(source.timestamp(), new SwitchStatus(Optional.empty()));
    }
}
