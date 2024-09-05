package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.hcc.device.DeviceState;
import com.homeclimatecontrol.hcc.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A switch that turns off when the first participating virtual switch is turned on, and turns off when the last
 * participating virtual switch is turned off.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2023
 */
public class StackingCqrsSwitch implements CqrsSwitch<String> {

    private final Logger logger = LogManager.getLogger();

    public final String address;

    private final CqrsSwitch<?> target;

    /**
     * The key is whatever is provided to {@link #getSwitch(String)}, the value is the corresponding proxy.
     */
    private final Map<String, StackingCqrsSwitch.SwitchProxy> address2switch = new TreeMap<>();

    /**
     * Switches in "on" state.
     */
    private final Set<CqrsSwitch<?>> demand = new TreeSet<>();

    /**
     * Create an instance.
     * @param address An address for this switch, likely something human readable.
     * @param target Switch that
     */
    public StackingCqrsSwitch(String address, CqrsSwitch<?> target) {
        this.address = address;
        this.target = target;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public boolean isAvailable() {
        return target.isAvailable();
    }

    @Override
    public DeviceState<Boolean> getState() {
        return target.getState();
    }

    @Override
    public DeviceState<Boolean> setState(Boolean state) {
        throw new UnsupportedOperationException("This switch is only controlled via its virtual switches, use getSwitch(address) to get one");
    }

    @Override
    public Flux<Signal<DeviceState<Boolean>, String>> getFlux() {
        return target.getFlux();
    }

    @Override
    public void close() throws Exception {
        target.close();
    }
    /**
     * Obtain a virtual switch instance.
     *
     * @param address Virtual switch address.
     *
     * @return If the address is unknown, a new switch is created, otherwise existing instance is returned.
     */
    public synchronized CqrsSwitch<String> getSwitch(String address) {

        logger.debug("getSwitch:{}", address);
        return address2switch.computeIfAbsent(address, k -> createSwitch(address));
    }

    private SwitchProxy createSwitch(String address) {

        logger.debug("createSwitch:{}", address);
        return new SwitchProxy(address);
    }

    public class SwitchProxy implements CqrsSwitch<String> {

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
        public void close() throws Exception {

        }

        @Override
        public boolean isAvailable() {
            return target.isAvailable();
        }

        @Override
        public DeviceState<Boolean> getState() {

            var targetState = target.getState();

            return new DeviceState<>(
                    address,
                    isAvailable(),
                    state,
                    targetState.actual,
                    targetState.queueDepth
            );
        }

        /**
         * Set this switch's {@link #state}, and the {@link #target} state if necessary.
         */
        @Override
        public DeviceState<Boolean> setState(Boolean state) {
            this.state = state;

            if (Boolean.TRUE.equals(state)) {
                demand.add(this);
            } else {
                demand.remove(this);
            }

            logger.debug("{}: state={} demand={}", address, state, demand.size());

            return target.setState(!demand.isEmpty());
        }

        @Override
        public Flux<Signal<DeviceState<Boolean>, String>> getFlux() {
            throw new UnsupportedOperationException("likely programming error, this class should not be accessible");
        }
    }
}
