package net.sf.dz3r.view.swing.dashboard;

import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.signal.health.SystemStatus;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityPanel;
import net.sf.dz3r.view.swing.ScreenDescriptor;
import reactor.core.publisher.Flux;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;

public class DashboardPanel extends EntityPanel<SystemStatus, Void> {

    private final InstrumentCluster ic;

    private final JPanel sensorPanel = new JPanel();
    private final JPanel switchPanel = new JPanel();
    private final JPanel connectorPanel = new JPanel();
    private final JPanel collectorPanel = new JPanel();
    private final JPanel hvacDevicePanel = new JPanel();

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
