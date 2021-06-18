package net.sf.dz4.device.actuator.pi.autohat;

import com.homeclimatecontrol.autohat.AutomationHAT;
import com.homeclimatecontrol.autohat.Relay;
import com.homeclimatecontrol.autohat.pi.PimoroniAutomationHAT;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.Switch;

import java.io.IOException;
import java.util.List;

/**
 * Wrapper to convert independent abstractions of {@link com.homeclimatecontrol.autohat.AutomationHAT}
 * into ones acceptable for DZ.
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
    public List<Switch> relay() {
        return List.of(
                switchWrapper(hat.relay().get(0), "R0"),
                switchWrapper(hat.relay().get(1), "R1"),
                switchWrapper(hat.relay().get(2), "R2"));
    }

    private Switch switchWrapper(Relay relay, String name) {
        return new RelayWrapper(relay, name);
    }

    private static class RelayWrapper implements Switch {

        private final Relay target;
        private final String name;

        public RelayWrapper(Relay target, String name) {
            this.target = target;
            this.name = name;
        }

        @Override
        public String getAddress() {
            return name;
        }

        @Override
        public boolean getState() throws IOException {

            // This value will be missing just on startup, unlikely to be a problem
            return target.read().orElse(false);
        }

        @Override
        public void setState(boolean state) throws IOException {
            target.write(state);
        }

        @Override
        public void addConsumer(DataSink<Boolean> consumer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeConsumer(DataSink<Boolean> consumer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JmxDescriptor getJmxDescriptor() {
            throw new UnsupportedOperationException();
        }
    }
}
