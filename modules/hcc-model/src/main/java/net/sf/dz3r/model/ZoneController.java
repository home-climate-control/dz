package net.sf.dz3r.model;

import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;
import net.sf.dz3r.device.actuator.damper.DamperController;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Accepts signals from {@link Zone zones} and issues signals to {@link UnitController} and {@link DamperController}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
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

    /**
     * Unique sequence number for {@link #process(Signal)} call.
     */
    private final AtomicLong processCallCount = new AtomicLong();

    public ZoneController(Collection<Zone> zones) {

        logger.info("Zones configured:");
        this.zoneMap = Flux
                .fromIterable(zones)
                .doOnNext(z -> logger.info("  {}", z.getAddress()))
                .collectMap(Zone::getAddress, z -> z)
                .block();

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

        if (zoneMap.containsKey(signal.payload())) {
            return true;
        }

        // Unless this is done, computeDemand() will be off
        // warn() is warranted here, this likely indicates a programming or configuration problem
        logger.warn("Alien zone '{}', signal dropped: {}", signal.payload(), signal);

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

        zone2status.put(signal.payload(), signal);
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

        // Log messages from different calls often get interleaved
        final var callId = Long.toHexString(processCallCount.getAndIncrement());

        var countNonError = new AtomicInteger();
        var countEnabled = new AtomicInteger();
        var countUnhappy = new AtomicInteger();
        var countUnhappyVoting = new AtomicInteger();

        // VT: FIXME: Lower these four log statements to TRACE later. Keep in mind that not all of them will show up all the time.

        var nonError = zone2status
                .entrySet()
                .stream()
                .peek(s -> logger.debug("callId={} process/signal: {}", callId, s))
                .filter(kv -> !kv.getValue().isError())
                .peek(ignored -> logger.debug("callId={} process/non-error: {}", callId, countNonError.incrementAndGet()));

        var enabled = nonError
                .filter(kv -> kv.getValue().getValue().settings().isEnabled())
                .peek(ignored -> logger.debug("callId={} process/enabled: {}", callId, countEnabled.incrementAndGet()));

        var unhappy = enabled
                .filter(kv -> kv.getValue().getValue().callingStatus().calling())
                .peek(ignored -> logger.debug("callId={} process/unhappy: {}", callId, countUnhappy.incrementAndGet()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var unhappyVoting = unhappy
                .entrySet()
                .stream()
                .filter(kv -> kv.getValue().getValue().settings().isVoting())
                .peek(ignored -> logger.debug("callId={} process/unhappy-voting: {}", callId, countUnhappyVoting.incrementAndGet()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // "Bump" is letting the thermostat know that the unit is starting and they may want to reconsider their
        // calling status
        var needBump = lastKnownCalling == 0 && !unhappyVoting.isEmpty();
        lastKnownCalling = countUnhappyVoting.get();

        logger.debug("callId={} unhappy={}, unhappyVoting={}, needBump={}, signal={}", callId, countUnhappy, countUnhappyVoting, needBump, signal);

        if (needBump) {
            raise();
        }

        var demand = computeDemand(unhappy, unhappyVoting);

        return new Signal<>(signal.timestamp(), new UnitControlSignal(demand, null));
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
                .filter(z -> z.getSettings().isEnabled())
                .filter(z -> z.getSettings().isVoting())
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
                .map(e -> e.getValue().callingStatus().demand())
                .reduce(Double::sum).orElse(0d);
    }

    private void raise() {

        Flux
                .fromIterable(zoneMap.values())
                .subscribe(Zone::raise);
    }
}
