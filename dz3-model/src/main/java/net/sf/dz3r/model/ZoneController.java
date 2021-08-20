package net.sf.dz3r.model;

import net.sf.dz3r.controller.SignalProcessor;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.UnitControlSignal;
import net.sf.dz3r.signal.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Accepts signals from {@link Zone zones} and issues signals to Unit and Damper Controller.
 *
 * VT: FIXME: Augment the description with links once those entities are ported to reactive streams.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZoneController implements SignalProcessor<ZoneStatus, UnitControlSignal, String> {

    private final Logger logger = LogManager.getLogger();

    /**
     * Mapping from zone name to latest zone signal.
     */
    private final Map<String, Signal<ZoneStatus, String>> zone2status = new TreeMap<>();

    /**
     * Accept zone signals, emit unit control signal.
     *
     * @param in Flux of {@link Zone#compute(Flux) zone} signals. The payload string is the zone name.
     *
     * @return {@link UnitController#compute(Flux) Unit control} signal. No payload.
     */
    @Override
    public Flux<Signal<UnitControlSignal, String>> compute(Flux<Signal<ZoneStatus, String>> in) {

        return in
                .doOnNext(this::capture)
                .map(this::process);
    }

    private void capture(Signal<ZoneStatus, String> signal) {
        zone2status.put(signal.payload, signal);
    }

    private int lastKnownCalling = 0;

    /**
     * Emit the control signal.
     *
     * @param signal Incoming zone signal.
     *
     * @return Unit control signal with no payload.
     */
    private Signal<UnitControlSignal, String> process(Signal<ZoneStatus, String> signal) {

        var nonError = zone2status
                .entrySet()
                .stream()
                .filter(kv -> !kv.getValue().isError());

        // Careful here
        // https://github.com/home-climate-control/dz/issues/195
        var votingEnabledCount = new AtomicInteger();

        var enabled = nonError
                .filter(kv -> kv.getValue().getValue().settings.enabled)
                .peek(kv -> {
                    if (Boolean.TRUE.equals(kv.getValue().getValue().settings.voting)) {
                        votingEnabledCount.incrementAndGet();
                    }
                });

        var unhappy = enabled
                .filter(kv -> kv.getValue().getValue().status.calling)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        logger.debug("votingEnabledCount={}", votingEnabledCount);

        var unhappyVoting = unhappy
                .entrySet()
                .stream()
                .filter(kv -> kv.getValue().getValue().settings.voting)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var unhappyCount = unhappy.size();
        var unhappyVotingCount = unhappyVoting.size();

        // "Bump" is letting the thermostat know that the unit is starting and they may want to reconsider their
        // calling status
        var needBump = lastKnownCalling == 0 && unhappyVoting.size() > 0;
        lastKnownCalling = unhappyVoting.size();

        logger.debug("unhappy={}, unhappyVoting={}, needBump={}, signal={}", unhappyCount, unhappyVotingCount, needBump, signal);

        // VT: FIXME: raise() is impossible with streaming control model, how to introduce a feedback loop?

        var demand = computeDemand(unhappy, unhappyVoting, votingEnabledCount.intValue() == 0);

        return new Signal<>(signal.timestamp, new UnitControlSignal(demand, null));
    }

    private double computeDemand(
            Map<String, Signal<ZoneStatus, String>> unhappy,
            Map<String, Signal<ZoneStatus, String>> unhappyVoting,
            boolean includeNonVoting) {

        var demandTotal = computeDemand(unhappy);
        var demandVoting = computeDemand(unhappyVoting);

        logger.info("demandVoting={}, includeNonVoting={}", demandVoting, includeNonVoting);

        if (demandVoting == 0.0 && !includeNonVoting) {
            // Nothing to do, moving on
            logger.debug("no voting demand, totalDemand=0");
            return 0;
        }

        double demandFinal;

        if (demandVoting * demandTotal >= 0 && Math.abs(demandTotal) > Math.abs(demandVoting)) {
            demandFinal = demandTotal;
        } else {
            demandFinal = demandVoting;
        }

        logger.debug("demand: total={}, voting={}, final={}", demandTotal, demandVoting, demandFinal);

        return demandFinal;
    }

    private double computeDemand(Map<String, Signal<ZoneStatus, String>> source) {

        return source
                .values()
                .stream()
                .map(e -> e.getValue().status.demand)
                .reduce(Double::sum).orElse(0d);
    }
}
