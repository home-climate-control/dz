package net.sf.dz3r.device.actuator.pi.autohat;

import com.homeclimatecontrol.autohat.AutomationHAT;
import com.homeclimatecontrol.autohat.Relay;
import com.homeclimatecontrol.autohat.pi.PimoroniAutomationHAT;
import net.sf.dz3r.device.actuator.AbstractSwitch;
import net.sf.dz3r.device.actuator.Switch;

import java.io.IOException;
import java.util.List;

/**
 * Wrapper to convert independent abstractions of {@link AutomationHAT}
 * into ones acceptable for DZ, and make them available at a proper point in DZ entities' lifecycle.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class AutomationHatWrapper {

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
    public List<Switch<String>> relay() {
        return List.of(
                switchWrapper(hat.relay().get(0), "R0"),
                switchWrapper(hat.relay().get(1), "R1"),
                switchWrapper(hat.relay().get(2), "R2"));
    }

    private Switch<String> switchWrapper(Relay relay, String name) {
        return new RelayWrapper(relay, name);
    }

    private static class RelayWrapper extends AbstractSwitch<String> {

        private final Relay target;

        public RelayWrapper(Relay target, String name) {
            super(name);
            this.target = target;
        }

        @Override
        protected void setStateSync(boolean state) throws IOException {
            target.write(state);
        }

        @Override
        protected boolean getStateSync() throws IOException {
            // This value will be missing just on startup, unlikely to be a problem
            return target.read().orElse(false);
        }
    }
}
