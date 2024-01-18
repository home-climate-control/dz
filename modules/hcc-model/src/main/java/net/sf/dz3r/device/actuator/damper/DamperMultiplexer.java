package net.sf.dz3r.device.actuator.damper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

/**
 * Damper multiplexer.
 *
 * Allows to control several physical dampers via one logical one. Each of controlled dampers
 * can be calibrated individually.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class DamperMultiplexer<A extends Comparable<A>> extends AbstractDamper<A> {

    /**
     * Dampers to control.
     */
    private final Set<Damper<?>> dampers = new HashSet<>();

    /**
     * Create an instance with a default park position.
     *
     * @param address Address to use.
     * @param dampers Set of dampers to control.
     *
     */
    protected DamperMultiplexer(A address, Set<Damper<?>> dampers) {
        this(address, dampers, null);
    }

    public DamperMultiplexer(A address, Set<Damper<?>> dampers, Double parkPosition) {
        super(address);

        if (dampers.isEmpty()) {
            logger.warn("no dampers to multiplex, is your configuration complete?");
        }

        this.dampers.addAll(dampers);

        if (parkPosition != null) {
            setParkPosition(parkPosition);
        }
    }

    @Override
    public Mono<Double> set(double position) {

        return Mono.create(sink -> {

            try {

                Flux.fromIterable(dampers)
                        .map(d -> d.set(position))
                        .map(Mono::block)
                        .blockLast();

                // If we've made it here, we're golden
                sink.success(position);

            } catch (Exception ex) {
                logger.error("setPosition(" + position + ") failed", ex);
                sink.error(ex);
            }
        });
    }

    @Override
    public final Mono<Double> park() {

        return Mono.create(sink -> {

            try {

                Flux.fromIterable(dampers)
                        .map(Damper::park)
                        .map(Mono::block)
                        .blockLast();

                // If we've made it here, we're golden
                // The position is bogus, all the dampers in the set may have had different park positions
                sink.success(getParkPosition());

            } catch (Exception ex) {
                logger.error("park() failed", ex);
                sink.error(ex);
            }
        });
    }
}
