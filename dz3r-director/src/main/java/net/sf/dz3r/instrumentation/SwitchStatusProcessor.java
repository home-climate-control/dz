package net.sf.dz3r.instrumentation;

import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.signal.Signal;
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
public class SwitchStatusProcessor implements SignalProcessor<Switch.State, SwitchStatus, String> {

    private final Logger logger = LogManager.getLogger();

    private final String id;

    public SwitchStatusProcessor(String id) {
        this.id = id;

        logger.info("created switch status processor for id={}", id);
    }

    @Override
    public Flux<Signal<SwitchStatus, String>> compute(Flux<Signal<Switch.State, String>> in) {
        return in.map(this::compute);
    }

    private Signal<SwitchStatus, String> compute(Signal<Switch.State, String> source) {

        if (source.isError()) {
            // Nothing else matters
            return new Signal<>(source.timestamp, null, null, source.status, source.error);
        }

        // VT: FIXME: Pass/fail is the only thing of interest right now
        return new Signal<>(source.timestamp, new SwitchStatus(Optional.empty()));
    }
}
