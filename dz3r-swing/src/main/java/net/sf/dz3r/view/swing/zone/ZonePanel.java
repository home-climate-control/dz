package net.sf.dz3r.view.swing.zone;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.ZoneStatus;
import net.sf.dz3r.view.swing.EntityPanel;
import net.sf.dz3r.view.swing.ScreenDescriptor;
import reactor.core.publisher.Flux;

import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;

public class ZonePanel extends EntityPanel<ZoneStatus, Void> {

    private final String name;

    private Signal<Double, Void> sensorSignal;
    private HvacMode hvacMode;

    public ZonePanel(String name) {
        this.name = name;
    }

    @Override
    protected void createControls(JPanel controls, GridBagLayout layout, GridBagConstraints cs) {

    }

    @Override
    public void setFontSize(ScreenDescriptor screenDescriptor) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    public void subscribeSensor(Flux<Signal<Double, Void>> sensorFlux) {
        sensorFlux.subscribe(this::consumeSensorSignal);
    }

    private void consumeSensorSignal(Signal<Double, Void> sensorSignal) {
        this.sensorSignal = sensorSignal;
        logger.info("sensorSignal: {}", sensorSignal);
        update();
    }

    public void subscribeMode(Flux<Signal<HvacMode, Void>> hvacModeFlux) {
        hvacModeFlux.subscribe(this::getMode);
    }

    private void getMode(Signal<HvacMode, Void> hvacModeSignal) {
        this.hvacMode = hvacModeSignal.getValue();
        logger.info("hvacMode: {}", hvacMode);
        update();
    }
}
