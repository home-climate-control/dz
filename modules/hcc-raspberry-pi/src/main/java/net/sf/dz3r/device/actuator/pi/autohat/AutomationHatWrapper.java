package net.sf.dz3r.device.actuator.pi.autohat;

import com.homeclimatecontrol.autohat.AutomationHAT;
import com.homeclimatecontrol.autohat.Relay;
import com.homeclimatecontrol.autohat.pi.PimoroniAutomationHAT;
import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.device.actuator.CqrsSwitch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

/**
 * Wrapper to convert independent abstractions of {@link AutomationHAT}
 * into ones acceptable for DZ, and make them available at a proper point in DZ entities' lifecycle.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class AutomationHatWrapper {

    private static final Logger logger = LogManager.getLogger(AutomationHatWrapper.class);
    private static AutomationHatWrapper instance;

    private final AutomationHAT hat;

    public static synchronized AutomationHatWrapper getInstance() throws IOException {

        if (instance == null) {
            instance = new AutomationHatWrapper();
        }

        return instance;
    }

    /**
     * Create an instance.
     *
     * There's no need for arguments, Automation HAT is a singleton.
     */
    private AutomationHatWrapper() throws IOException {
        hat = PimoroniAutomationHAT.getInstance();
    }

    /**
     * Get all the relays.
     *
     * @return All the relays as a list.
     */
    public List<CqrsSwitch<String>> relay() {
        return List.of(
                switchWrapper(hat.relay().get(0), "R0"),
                switchWrapper(hat.relay().get(1), "R1"),
                switchWrapper(hat.relay().get(2), "R2"));
    }

    private CqrsSwitch<String> switchWrapper(Relay relay, String name) {
        return new RelayWrapper(relay, name);
    }

    private static class RelayWrapper implements CqrsSwitch<String> {

        private final String name;
        private final Relay target;

        private boolean available = true;

        private Boolean requested;
        private Boolean actual;

        public RelayWrapper(Relay target, String name) {
            this.name = name;
            this.target = target;
        }

        private boolean readState() throws IOException {
            // This value will be missing just on startup, unlikely to be a problem
            return target.read().orElse(false);
        }

        @Override
        public String getAddress() {
            return name;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public DeviceState<Boolean> getState() {

            if (available) {

                try {
                    actual = readState();
                } catch (IOException ex) {
                    available = false;
                    logger.error("{}: readState() failed, marking device unavailable", getAddress(), ex);
                }
            }

            return new DeviceState<>(
                    name,
                    isAvailable(),
                    requested,
                    actual,
                    0
            );
        }

        @Override
        public DeviceState<Boolean> setState(Boolean state) {

            this.requested = state;

            try {
                target.write(state);
                this.actual = state;
                available = true;
            } catch (IOException ex) {
                available = false;
                logger.error("{}: setState({}) failed, marking device unavailable", getAddress(), state, ex);
            }

            return getState();
        }

        @Override
        public Flux<Signal<DeviceState<Boolean>, String>> getFlux() {
            throw new UnsupportedOperationException("likely programming error, this class should not be accessible");
        }

        @Override
        public void close() throws Exception {

        }
    }
}
