package net.sf.dz3r.view.swing.dashboard;

import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.health.SensorStatus;
import net.sf.dz3r.signal.health.SwitchStatus;
import net.sf.dz3r.signal.health.SystemStatus;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityPanel;
import net.sf.dz3r.view.swing.ScreenDescriptor;
import reactor.core.publisher.Flux;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class DashboardPanel extends EntityPanel<SystemStatus, Void> {

    private final InstrumentCluster ic;

    private final JPanel sensorPanel = new JPanel();
    private final JPanel switchPanel = new JPanel();
    private final JPanel connectorPanel = new JPanel();
    private final JPanel collectorPanel = new JPanel();
    private final JPanel hvacDevicePanel = new JPanel();

    private final Map<String, JPanel> sensors = new TreeMap<>();
    private final Map<String, JPanel> switches = new TreeMap<>();

    /**
     * Status accumulator.
     *
     * This object gets updated and then rendered every time an {@link #update()} comes.
     */
    private final SystemStatus currentStatus = new SystemStatus(
            new TreeMap<>(),
            new TreeMap<>(),
            new TreeMap<>(),
            new TreeMap<>(),
            new TreeMap<>());

    public DashboardPanel(InstrumentCluster ic, ScreenDescriptor screenDescriptor) {

        this.ic = ic;

        setFontSize(screenDescriptor);
        initGraphics();
    }

    private void initGraphics() {

        setBackground(ColorScheme.offMap.background);
        setBorder(new TitledBorder("Instrument Cluster"));
        ((TitledBorder) getBorder()).setTitleColor(Color.WHITE);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        sensorPanel.setBorder(new TitledBorder("Sensors"));
        switchPanel.setBorder(new TitledBorder("Switches"));
        connectorPanel.setBorder(new TitledBorder("Connectors"));
        collectorPanel.setBorder(new TitledBorder("Collectors"));
        hvacDevicePanel.setBorder(new TitledBorder("HVAC Devices"));

        sensorPanel.setLayout(new BoxLayout(sensorPanel, BoxLayout.X_AXIS));
        switchPanel.setLayout(new BoxLayout(switchPanel, BoxLayout.X_AXIS));
//        connectorPanel.setLayout(new BoxLayout(connectorPanel, BoxLayout.X_AXIS));
//        collectorPanel.setLayout(new BoxLayout(collectorPanel, BoxLayout.X_AXIS));
//        hvacDevicePanel.setLayout(new BoxLayout(hvacDevicePanel, BoxLayout.X_AXIS));

        Flux
                .just(
                        sensorPanel,
                        switchPanel,
                        connectorPanel,
                        collectorPanel,
                        hvacDevicePanel)
                .doOnNext(panel -> panel.setBackground(ColorScheme.offMap.background))
                .doOnNext(panel -> ((TitledBorder) panel.getBorder()).setTitleColor(Color.WHITE))
                .doOnNext(this::add)
                .subscribe();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No special handling
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // No special handling yet
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No special handling
    }

    @Override
    protected void consumeSignalValue(SystemStatus status) {
        // No special handling
    }

    @Override
    protected void update() {

        var signal = getSignal();

        switch (signal.status) {
            case OK -> setTitleColor(Color.WHITE);
            case FAILURE_PARTIAL -> setTitleColor(Color.ORANGE);
            case FAILURE_TOTAL -> setTitleColor(Color.RED);
        }

        if (signal.isError()) {
            logger.warn("don't know how to handle: {}", signal);
            return;
        }

        renderSensors(signal.getValue().sensors());
        renderSwitches(signal.getValue().switches());
    }

    private void renderSensors(Map<String, Signal<SensorStatus, Void>> source) {

        var currentCount = sensors.size();

        // There is likely just one entry, but let's be generic

        for (var kv: source.entrySet()) {
            renderSensor(
                    sensors.computeIfAbsent(kv.getKey(), k -> createSensorBox(kv.getKey())),
                    kv.getKey(),
                    kv.getValue());
        }

        var newCount = sensors.size();

        if (newCount != currentCount) {
            // Looks like new sensor has just woken up
            sensorPanel.removeAll();

            for (var kv: sensors.entrySet()) {
                sensorPanel.add(kv.getValue());
            }
        }
    }

    private void renderSwitches(Map<String, Signal<SwitchStatus, String>> source) {

        var currentCount = switches.size();

        // There is likely just one entry, but let's be generic

        for (var kv: source.entrySet()) {
            renderSwitch(
                    switches.computeIfAbsent(kv.getKey(), k -> createSensorBox(kv.getKey())),
                    kv.getKey(),
                    kv.getValue());
        }

        var newCount = switches.size();

        if (newCount != currentCount) {
            // Looks like new sensor has just woken up
            switchPanel.removeAll();

            for (var kv: switches.entrySet()) {
                switchPanel.add(kv.getValue());
            }
        }
    }

    private JPanel createSensorBox(String id) {

        var result = new JPanel();

        result.setBackground(ColorScheme.offMap.background);
        result.setToolTipText(id);
        result.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));

        return result;
    }

    private void renderError(JPanel p, String id, Throwable t) {

        p.setBackground(ColorScheme.offMap.error);
        p.setToolTipText("<html>" + id + "<hr>" + t.getClass().getCanonicalName() + "<br>" + t.getMessage() + "</html>");
    }

    private void renderSensor(JPanel p, String id, Signal<SensorStatus, Void> signal) {

        if (signal.isError()) {
            renderError(p, id, signal.error);
            return;
        }

        p.setBackground(ColorScheme.offMap.green);
        p.setToolTipText("<html>"
                + id
                + "<hr>" + Optional.ofNullable(signal.getValue().resolution())
                .map(r -> "Resolution: " + r)
                .orElse("")
                + "</html>");
    }

    private void renderSwitch(JPanel p, String id, Signal<SwitchStatus, String> signal) {

        if (signal.isError()) {
            renderError(p, id, signal.error);
            return;
        }

        p.setBackground(ColorScheme.offMap.green);
        p.setToolTipText("<html>" + id + "</html>");
    }

    private void setTitleColor(Color c) {

        Flux
                .just(
                        sensorPanel,
                        switchPanel,
                        connectorPanel,
                        collectorPanel,
                        hvacDevicePanel)
                .doOnNext(panel -> ((TitledBorder) panel.getBorder()).setTitleColor(c))
                .subscribe();
    }

    @Override
    protected void createControls(JPanel controls, GridBagLayout layout, GridBagConstraints cs) {
        throw new IllegalStateException("Not Implemented Here");
    }

    @Override
    public void setFontSize(ScreenDescriptor screenDescriptor) {

        sensorPanel.setFont(screenDescriptor.fontSetpoint);
        switchPanel.setFont(screenDescriptor.fontSetpoint);
        connectorPanel.setFont(screenDescriptor.fontSetpoint);
        collectorPanel.setFont(screenDescriptor.fontSetpoint);
        hvacDevicePanel.setFont(screenDescriptor.fontSetpoint);
    }
}
