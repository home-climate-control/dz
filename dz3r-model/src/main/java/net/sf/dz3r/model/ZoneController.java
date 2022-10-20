package net.sf.dz3r.model;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
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
     * Mapping from zone name to the zone itself.
     *
     * This map is created at instantiation time and never changes.
     */
    private final Map<String, Zone> zoneMap;

    /**
     * Mapping from zone name to the latest zone signal.
     */
    private final Map<String, Signal<ZoneStatus, String>> zone2status = new TreeMap<>();

    public ZoneController(Collection<Zone> zones) {

        this.zoneMap = zones
                .stream()
                .collect(Collectors.toMap(
                        Zone::getAddress,
                        z -> z,
                        (s, s2) -> s,
                        TreeMap::new));

        logger.info("Zones configured:");
        zoneMap.keySet().forEach(z -> logger.info("  {}", z));

        if (zones.size() > zoneMap.size()) {

            logger.error("Discrepancy between zone names and zones");
            logger.error("Check zone and thermostat configuration values, this is usually a copypaste error");
            zones.forEach(z -> logger.error("  {}", z));

            throw new IllegalArgumentException("Redundant zones? (see the logs above)");
        }
    }

    /**
     * Accept zone signals, emit unit control signal.
     *
     * @param in Flux of {@link Zone#compute(Flux) zone} signals. The payload string is the zone name.
     *
     * @return {@link UnitController#compute(Flux) Unit control} signal. No payload.
     */
    @Override
    public Flux<Signal<UnitControlSignal, String>> compute(Flux<Signal<ZoneStatus, String>> in) {

        logger.debug("compute()");

        return in
                .filter(this::isOurs)
                .doOnNext(this::capture)
                .map(this::process);
    }

    /**
     * Check if the signal belongs to this zone controller.
     *
     * @return {@code true} if this is our signal.
     */
    private boolean isOurs(Signal<ZoneStatus, String> signal) {

        if (zoneMap.containsKey(signal.payload)) {
            return true;
        }

        // Unless this is done, computeDemand() will be off
        // warn() is warranted here, this likely indicates a programming or configuration problem
        logger.warn("Alien zone '{}', signal dropped: {}", signal.payload, signal);

        return false;
    }

    /**
     * Capture the signal to get an idea about the big picture.
     *
     * Signals from zones not in {@link #zoneMap} will be dropped on the floor.
     *
     * @param signal Incoming signal.
     */
    private void capture(Signal<ZoneStatus, String> signal) {

        logger.debug("capture: {}", signal);

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

        // VT: NOTE: private method, it is safe to assume that alien signals have been filtered out by isOurs()

        var nonError = zone2status
                .entrySet()
                .stream()
                .filter(kv -> !kv.getValue().isError());

        var enabled = nonError
                .filter(kv -> kv.getValue().getValue().settings.enabled);

        var unhappy = enabled
                .filter(kv -> kv.getValue().getValue().thermostatStatus.calling)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

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

        // VT: FIXME: implement bump() and call it here

        var demand = computeDemand(unhappy, unhappyVoting);

        return new Signal<>(signal.timestamp, new UnitControlSignal(demand, null));
    }

    /**
     * Find out how many zones are both enabled and voting.
     *
     * @return The count.
     */
    private long getVotingEnabledCount() {

        // This better be done on a static source - not all values may be available from the stream at startup
        return zoneMap
                .values()
                .stream()
                .filter(z -> z.getSettings().enabled)
                .filter(z -> z.getSettings().voting)
                .count();
    }

    private double computeDemand(
            Map<String, Signal<ZoneStatus, String>> unhappy,
            Map<String, Signal<ZoneStatus, String>> unhappyVoting) {

        var demandTotal = computeDemand(unhappy);
        var demandVoting = computeDemand(unhappyVoting);

        // Careful here
        // https://github.com/home-climate-control/dz/issues/195
        var votingEnabledCount = getVotingEnabledCount();
        var includeNonVoting = votingEnabledCount == 0;


        logger.debug("demandVoting={}, votingEnabledCount={}, includeNonVoting={}", demandVoting, votingEnabledCount, includeNonVoting);

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
                .map(e -> e.getValue().thermostatStatus.demand)
                .reduce(Double::sum).orElse(0d);
    }
}
