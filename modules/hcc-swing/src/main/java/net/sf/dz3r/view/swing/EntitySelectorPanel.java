package net.sf.dz3r.view.swing;

import com.homeclimatecontrol.hcc.model.ZoneSettings;
import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.SchedulePeriod;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import net.sf.dz3r.view.swing.dashboard.DashboardCell;
import net.sf.dz3r.view.swing.dashboard.DashboardPanel;
import net.sf.dz3r.view.swing.sensor.SensorCell;
import net.sf.dz3r.view.swing.sensor.SensorPanel;
import net.sf.dz3r.view.swing.unit.UnitCell;
import net.sf.dz3r.view.swing.unit.UnitPanel;
import net.sf.dz3r.view.swing.zone.ZoneCell;
import net.sf.dz3r.view.swing.zone.ZonePanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class EntitySelectorPanel extends JPanel implements KeyListener {

    private final transient Logger logger = LogManager.getLogger();

    private final transient Config config;

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


    public EntitySelectorPanel(ReactiveConsole.Config consoleConfig, ScreenDescriptor screenDescriptor) {

        this.config = new Config(consoleConfig, screenDescriptor);

        init(consoleConfig);
    }

    private void init(ReactiveConsole.Config config) {

        initDashboard(config.ic());
        initDirectors(config.directors());
        initSensors(config.sensors());

        logger.info("Configured {} pairs out of {} directors and {} sensors", entities.size(), config.directors().size(), config.sensors().size());

        initGraphics();
    }

    private void initDashboard(InstrumentCluster ic) {
        entities.add(createDashboardPair(ic));
    }

    private CellAndPanel<?,?> createDashboardPair(InstrumentCluster ic) {

        var cell = new DashboardCell();
        var panel = new DashboardPanel(ic, config.screen);

        var icFlux = ic.getFlux();

        icFlux.subscribe(cell::consumeSignal);
        icFlux.subscribe(panel::consumeSignal);

        return new CellAndPanel<>(cell, panel);
    }

    private void initDirectors(Set<UnitDirector> initSet) {

        // VT: NOTE: sort() the units

        Flux.fromIterable(initSet)
                .sort()
                .flatMap(this::initUnit)
                .doOnNext(entities::add)
                .subscribe()
                .dispose();
    }

    /**
     * Create the UI element set containing unit and set of zone cell/panel pairs.
     *
     * @param source Unit director to get the configuration and feeds from.
     *
     * @return Flux of cell/panel pairs, unit first, zones second.
     */
    private Flux<CellAndPanel<?, ?>> initUnit(UnitDirector source) {

        var feed = source.getFeed();

        var unitPair = createUnitPair(source.getAddress(), feed.hvacDeviceFlux);
        var zonePairs = Flux
                .fromIterable(flip(feed.sensorFlux2zone).entrySet())
                .map(kv -> createZonePair(kv.getKey(), kv.getValue(), feed.aggregateZoneFlux, feed.hvacDeviceFlux, feed.scheduleFlux));

        return Flux.concat(Flux.just(unitPair), zonePairs);
    }

    private Map<Zone, Flux<Signal<Double, Void>>> flip(Map<Flux<Signal<Double, Void>>, Zone> source) {

        // feed.sensorFlux2zone is not sorted. Since we need zones sorted on the console, and zone names are
        // guaranteed to be unique, we can safely flip them here.

        // VT: NOTE: Why was the sensor flux the key, again? Any chance to flip it there to begin with?

        var result = new TreeMap<Zone, Flux<Signal<Double, Void>>>();

        for (var kv : source.entrySet()) {
            result.put(kv.getValue(), kv.getKey());
        }

        return result;
    }

    private CellAndPanel<HvacDeviceStatus, Void> createUnitPair(String address, Flux<Signal<HvacDeviceStatus, Void>> source) {

        var cell = new UnitCell(source);
        var panel = new UnitPanel(address, config.screen, source);

        return new CellAndPanel<>(cell, panel);
    }

    private CellAndPanel<ZoneStatus, Void> createZonePair(
            Zone zone,
            Flux<Signal<Double, Void>> sensorFlux,
            Flux<Signal<ZoneStatus, String>> aggregateZoneFlux,
            Flux<Signal<HvacDeviceStatus, Void>> hvacDeviceFlux,
            Flux<Map.Entry<String, Map.Entry<SchedulePeriod, ZoneSettings>>> scheduleFlux) {

        var zoneName = zone.getAddress();

        var thisZoneFlux = aggregateZoneFlux
                .filter(s -> zoneName.equals(s.payload))
                .map(s -> new Signal<ZoneStatus, Void>(s.timestamp, s.getValue(), null, s.status, s.error));
        var modeFlux = hvacDeviceFlux
                .filter(s -> s.getValue().command.mode != null)
                .map(s -> new Signal<HvacMode, Void>(s.timestamp, s.getValue().command.mode, null, s.status, s.error));
        var thisScheduleFlux = scheduleFlux
                .filter(s -> zoneName.equals(s.getKey()))
                .map(Map.Entry::getValue);

        var cell = new ZoneCell(
                zoneName,
                thisZoneFlux,
                modeFlux);
        var panel = new ZonePanel(
                zone, config.screen, config.console.measurementUnits(),
                thisZoneFlux,
                sensorFlux,
                modeFlux);

        return new CellAndPanel<>(cell, panel);
    }

    private void initSensors(Map<String, Flux<Signal<Double, Void>>> initSet) {

        Flux.fromIterable(initSet.entrySet())
                .map(this::initSensor)
                .doOnNext(entities::add)
                .subscribe()
                .dispose();
    }

    private CellAndPanel<?, ?> initSensor(Map.Entry<String, Flux<Signal<Double, Void>>> source) {

        var name = source.getKey();
        var signal = source.getValue();

        logger.debug("initSensor: {} => {}", name, signal);

        var cell = new SensorCell(name);
        var panel = new SensorPanel(name, config.screen);

        signal.subscribe(cell::consumeSignal);
        signal.subscribe(panel::consumeSignal);

        return new CellAndPanel<>(cell, panel);
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
            selectorPanel.add(pair.panel, String.valueOf(offset++));
        }

        setCurrentEntity(0);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No special handling
    }

    /**
     * Handle arrow right and left (change entity).
     */
    @Override
    @SuppressWarnings("squid:S1199")
    public void keyPressed(KeyEvent e) {

        // VT: NOTE: squid:S1199 - between this rule, and avoiding extra hassle and
        // violating consistency, guess what.

        ThreadContext.push("keyPressed");

        try {

            logger.info("{}", e::toString);

            switch (Character.toLowerCase(e.getKeyChar())) {

                case 'c', 'f' -> {

                    // Toggle between Celsius and Fahrenheit

                    for (var entity : entities) {
                        entity.panel.keyPressed(e);
                    }
                }

                case 'h', 'v', 'o', 's', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {

                    // H: toggle hold status
                    // V: toggle voting status
                    // O: toggle off status
                    // S: go back to schedule
                    // Digits: change dump priority

                    entities.get(currentEntityOffset).panel.keyPressed(e);
                }

                case KeyEvent.CHAR_UNDEFINED -> {

                    switch (e.getKeyCode()) {

                        case KeyEvent.VK_KP_LEFT, KeyEvent.VK_LEFT -> {

                            // Cycle displayed entity to the left

                            int entityOffset = currentEntityOffset - 1;

                            entityOffset = entityOffset < 0 ? entities.size() - 1 : entityOffset;

                            setCurrentEntity(entityOffset);
                        }

                        case KeyEvent.VK_KP_RIGHT, KeyEvent.VK_RIGHT -> {

                            // Cycle displayed entity to the right

                            int entityOffset = currentEntityOffset + 1;

                            entityOffset = entityOffset >= entities.size() ? 0 : entityOffset;

                            setCurrentEntity(entityOffset);
                        }


                        case KeyEvent.VK_KP_UP, KeyEvent.VK_UP, KeyEvent.VK_KP_DOWN, KeyEvent.VK_DOWN -> {

                            // Raise or lower setpoint for currently selected zone (if it is a zone)

                            entities.get(currentEntityOffset).panel.keyPressed(e);
                        }

                        default -> {
                            // Do nothing
                        }

                    }
                }
                default -> {
                    // Do nothing
                }
            }

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No special handling
    }

    /**
     * Change the currently displayed entity to the one with the given offset.
     *
     * @param entityOffset Offset of the zone to display.
     */
    private void setCurrentEntity(int entityOffset) {

        entities.get(currentEntityOffset).cell.setSelected(false);
        entities.get(entityOffset).cell.setSelected(true);

        cardLayout.show(selectorPanel, String.valueOf(entityOffset));

        currentEntityOffset = entityOffset;
    }

    public synchronized void setSize(ScreenDescriptor screenDescriptor) {

        for (var entity : entities) {
            entity.panel.setFontSize(screenDescriptor);
        }
    }

    private record Config(
            ReactiveConsole.Config console,
            ScreenDescriptor screen
    ) {

    }
}
