package net.sf.dz3r.view.swing;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.HvacDeviceStatus;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.ZoneStatus;
import net.sf.dz3r.view.swing.unit.UnitCell;
import net.sf.dz3r.view.swing.unit.UnitPanel;
import net.sf.dz3r.view.swing.zone.ZoneCell;
import net.sf.dz3r.view.swing.zone.ZonePanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EntitySelectorPanel extends JPanel implements KeyListener {

    private final transient Logger logger = LogManager.getLogger();

    private int currentEntityOffset = 0;
    private final transient List<CellAndPanel<?, ?>> entities = new ArrayList<>();

    /**
     * Panel to display {@link EntityPanel} instances.
     */
    private final JPanel selectorPanel = new JPanel();

    /**
     * Layout to control which entity is showing.
     */
    private final CardLayout cardLayout = new CardLayout();

    /**
     * Screen descriptor at start.
     *
     * Ugly hack, need to remove it someday.
     */
    private final ScreenDescriptor initialScreenDescriptor;

    public EntitySelectorPanel(Set<Object> initSet, ScreenDescriptor screenDescriptor) {
        this.initialScreenDescriptor = screenDescriptor;
        init(initSet);
    }

    private void init(Set<Object> initSet) {

        initUnits(initSet);
        initSensors(initSet);

        logger.info("Configured {} pairs out of {} init entries", entities.size(), initSet.size());

        initGraphics();
    }

    private void initUnits(Set<Object> initSet) {

        // VT: NOTE: sort() the units

        Flux.fromIterable(initSet)
                .sort()
                .filter(UnitDirector.class::isInstance)
                .map(UnitDirector.class::cast)
                .flatMap(this::initUnit)
                .doOnNext(entities::add)
                .subscribe()
                .dispose();
    }

    private Flux<CellAndPanel<?, ?>> initUnit(UnitDirector source) {

        var feed = source.getFeed();

        var unitPair = createUnitPair(source.getAddress(), feed.hvacDeviceFlux);
        var zonePairs = Flux
                .fromIterable(feed.sensorFlux2zone.entrySet())
                .map(kv -> createZonePair(kv.getValue(), kv.getKey(), feed.aggregateZoneFlux, feed.hvacDeviceFlux));

        return Flux.concat(Flux.just(unitPair), zonePairs);
    }

    private CellAndPanel<HvacDeviceStatus, Void> createUnitPair(String address, Flux<Signal<HvacDeviceStatus, Void>> source) {

        var cell = new UnitCell();
        var panel = new UnitPanel(address, initialScreenDescriptor);

        cell.subscribe(source);
        panel.subscribe(source);

        return new CellAndPanel<>(cell, panel);
    }

    private CellAndPanel<ZoneStatus, Void> createZonePair(
            Zone zone,
            Flux<Signal<Double, Void>> sensorFlux,
            Flux<Signal<ZoneStatus, String>> aggregateZoneFlux,
            Flux<Signal<HvacDeviceStatus, Void>> hvacDeviceFlux) {

        var zoneName = zone.getAddress();
        var cell = new ZoneCell(zoneName);
        var panel = new ZonePanel(zoneName);

        var thisZoneFlux = aggregateZoneFlux
                .filter(s -> zoneName.equals(s.payload))
                .map(s -> new Signal<ZoneStatus, Void>(s.timestamp, s.getValue(), null, s.status, s.error));
        var modeFlux = hvacDeviceFlux
                .map(s -> new Signal<HvacMode, Void>(s.timestamp, s.getValue().requested.mode, null, s.status, s.error));

        cell.subscribe(thisZoneFlux);
        panel.subscribe(thisZoneFlux);

        // Hack: passing the setpoint through the controller is not to simpler, so inject it
        panel.subscribeSensor(sensorFlux);

        // Hack: HVAC mode is not usually available to zones (and we'll have to overhaul all this for autochangeover anyway)
        cell.subscribeMode(modeFlux);
        panel.subscribeMode(modeFlux);

        return new CellAndPanel<>(cell, panel);
    }

    private void initSensors(Set<Object> initSet) {

        // VT: NOTE: sort() sensor panels, signals are not sortable

        Flux.fromIterable(initSet)
                .filter(Flux.class::isInstance)
                .map(Flux.class::cast)
                .flatMap(this::initSensor)
                .sort()
                .subscribe()
                .dispose();
    }

    private Flux<CellAndPanel<?, ?>> initSensor(Flux<?> source) {
        logger.warn("NOT IMPLEMENTED: initSensor({})", source);
        return Flux.empty();
    }

    private void initGraphics() {
        var layout = new GridBagLayout();
        var cs = new GridBagConstraints();

        this.setLayout(layout);

        // VT: NOTE: squid:S1199 - SonarLint is not smart enough to realize that these
        // blocks are for readability

        var selectorBar = new JPanel();

        {
            // Entity bar spans all the horizontal space available (as many cells as there are entities),
            // but the height is limited

            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = GridBagConstraints.REMAINDER;
            cs.gridheight = 1;
            cs.weightx = 1;
            cs.weighty = 0;

            layout.setConstraints(selectorBar, cs);
            this.add(selectorBar);
        }

        {
            // Entity panel is right below the entity bar

            cs.gridy++;

            // Constraints are different

            cs.fill = GridBagConstraints.BOTH;
            cs.gridheight = GridBagConstraints.REMAINDER;
            cs.weighty = 1;

            layout.setConstraints(selectorPanel, cs);
            this.add(selectorPanel);
        }

        selectorBar.setLayout(new GridLayout(1, entities.size()));
        selectorPanel.setLayout(cardLayout);

        var offset = 0;
        for (var pair : entities) {
            selectorBar.add(pair.cell);
            selectorPanel.add(pair.panel, "" + offset++);
        }

        setCurrentEntity(0);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public void keyPressed(KeyEvent e) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public void keyReleased(KeyEvent e) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Change the currently displayed entity to the one with the given offset.
     *
     * @param entityOffset Offset of the zone to display.
     */
    private void setCurrentEntity(int entityOffset) {

        entities.get(currentEntityOffset).cell.setSelected(false);
        entities.get(currentEntityOffset).cell.setSelected(true);

        cardLayout.show(selectorPanel, "" + entityOffset);

        currentEntityOffset = entityOffset;
    }

    public synchronized void setSize(ScreenDescriptor screenDescriptor) {

        for (var entity : entities) {
            entity.panel.setFontSize(screenDescriptor);
        }
    }
}
