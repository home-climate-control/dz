package net.sf.dz3r.device.actuator;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A switch that turns off when the first participating virtual switch is turned on, and turns off when the last
 * participating virtual switch is turned off.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2022
 */
public class StackingSwitch  implements Switch<String> {

    private final Logger logger = LogManager.getLogger();

    public final String address;

    private final Switch<?> target;

    /**
     * The key is whatever is provided to {@link #getSwitch(String)}, the value is the corresponding proxy.
     */
    private final Map<String, SwitchProxy> address2switch = new TreeMap<>();

    /**
     * Switches in "on" state.
     */
    private final Set<Switch<?>> demand = new TreeSet<>();

    /**
     * Create an instance.
     * @param address An address for this switch, likely something human readable.
     * @param target Switch that
     */
    public StackingSwitch(String address, Switch<?> target) {
        this.address = address;
        this.target = target;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public Flux<Signal<State, String>> getFlux() {
        return target.getFlux();
    }

    @Override
    public Mono<Boolean> setState(boolean state) {
        throw new UnsupportedOperationException("This switch is only controlled via its virtual switches, use getSwitch(address) to get one");
    }

    @Override
    public Mono<Boolean> getState() {
        return target.getState();
    }

    /**
     * Obtain a virtual switch instance.
     *
     * @param address Virtual switch address.
     *
     * @return If the address is unknown, a new switch is created, otherwise existing instance is returned.
     */
    public synchronized Switch<String> getSwitch(String address) {

        logger.debug("getSwitch:{}", address);
        return address2switch.computeIfAbsent(address, k -> createSwitch(address));
    }

    private SwitchProxy createSwitch(String address) {

        logger.debug("createSwitch:{}", address);
        return new SwitchProxy(address);
    }

    public class SwitchProxy implements Switch<String> {

        public final String address;
        private boolean state = false;


        public SwitchProxy(String address) {
            this.address = address;
        }

        @Override
        public String getAddress() {
            return address;
        }

        @Override
        public Flux<Signal<State, String>> getFlux() {
            throw new UnsupportedOperationException("likely programming error, this class should not be accessible");
        }

        /**
         * Set this switch's {@link #state}, and the {@link #target} state if necessary.
         */
        @Override
        public synchronized Mono<Boolean> setState(boolean state) {

            this.state = state;

            if (state) {
                demand.add(this);
            } else {
                demand.remove(this);
            }

            logger.debug("{}: state={} demand={}", address, state, demand.size());

            return target
                    .setState(!demand.isEmpty())
                    .map(ignored -> state);
        }

        @Override
        public Mono<Boolean> getState() {
            return Mono.just(state);
        }
    }
}
