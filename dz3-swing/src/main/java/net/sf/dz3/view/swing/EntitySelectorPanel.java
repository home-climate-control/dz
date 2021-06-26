package net.sf.dz3.view.swing;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.view.swing.thermostat.ThermostatPanel;
import net.sf.dz3.view.swing.thermostat.ZoneCell;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Panel that contains all {@link EntityPanel} instances in a {@link CardLayout}, and an indicator bar
 * that displays abbreviated status for all entities.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class EntitySelectorPanel extends JPanel implements KeyListener {

    private static final long serialVersionUID = 6400746493551083129L;

    private final transient Logger logger = LogManager.getLogger(getClass());

    private int currentEntityOffset = 0;
    private final transient Entity[] entities;

    /**
     * Panel to display bars for all entities.
     */
    private final JPanel selectorBar = new JPanel();

    /**
     * Panel to display {@link EntityPanel} instances.
     */
    private final JPanel selectorPanel = new JPanel();

    /**
     * Layout to control which entity is showing.
     */
    private final CardLayout cardLayout = new CardLayout();

    @SuppressWarnings("squid:S1199")
    public EntitySelectorPanel(Map<Object, JComponent> componentMap) {

        var layout = new GridBagLayout();
        var cs = new GridBagConstraints();

        this.setLayout(layout);

        // VT: NOTE: squid:S1199 - SonarLint is not smart enough to realize that these
        // blocks are for readability

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

        SortedMap<Thermostat, JComponent> thermostatMap = new TreeMap<>();

        for (Entry<Object, JComponent> entry : componentMap.entrySet()) {

            Object dataSource = entry.getKey();
            JComponent panel = entry.getValue();

            // VT: FIXME: Extend this to all sensors

            if (dataSource instanceof Thermostat) {
                thermostatMap.put((Thermostat) dataSource, panel);
            } else {
                logger.warn("don't know how to handle {}", dataSource.getClass());
            }
        }

        entities = new Entity[thermostatMap.size()];

        selectorBar.setLayout(new GridLayout(1, thermostatMap.size()));
        selectorPanel.setLayout(cardLayout);

        var offset = 0;
        for (Thermostat ts : thermostatMap.keySet()) {

            var panel = componentMap.get(ts);
            var cell = new ZoneCell(ts);
            var entity = new Entity(cell, (ThermostatPanel) panel);

            selectorBar.add(cell);
            selectorPanel.add(panel, "" + offset);

            entities[offset++] = entity;
        }

        setCurrentEntity(0);
    }

    private static class Entity {

        public final ZoneCell entityCell;
        public final EntityPanel entityPanel;

        public Entity(ZoneCell entityCell, EntityPanel entityPanel) {

            this.entityCell = entityCell;
            this.entityPanel = entityPanel;
        }
    }

    /**
     * Handle arrow right and left (change entity).
     */
    @Override
    @SuppressWarnings("squid:S1199")
    public synchronized void keyPressed(KeyEvent e) {

        // VT: NOTE: squid:S1199 - between this rule, and avoiding extra hassle and
        // violating consistency, guess what.

        ThreadContext.push("keyPressed");

        try {

            logger.info("{}", e::toString);

            switch (e.getKeyChar()) {

            case 'c':
            case 'C':
            case 'f':
            case 'F':

                // Toggle between Celsius and Fahrenheit

                // This must work for all entities
                for (Entity entity : entities) {
                    entity.entityPanel.keyPressed(e);
                }

                break;

            case 'h':
            case 'H':

                    // Toggle hold status

            case 'v':
            case 'V':

                    // Toggle voting status

            case 'o':
            case 'O':

                    // Toggle off status

            case 's':
            case 'S':

                // Go back to schedule

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':

                // Change dump priority

                entities[currentEntityOffset].entityPanel.keyPressed(e);
                break;

            case KeyEvent.CHAR_UNDEFINED:

                switch (e.getKeyCode()) {

                case KeyEvent.VK_KP_LEFT:
                case KeyEvent.VK_LEFT:

                    // Cycle displayed entity to the left

                    {
                        int entityOffset = currentEntityOffset - 1;

                        entityOffset = entityOffset < 0 ? entities.length - 1 : entityOffset;

                        setCurrentEntity(entityOffset);
                    }

                    break;

                case KeyEvent.VK_KP_RIGHT:
                case KeyEvent.VK_RIGHT:

                    // Cycle displayed entity to the right

                    {
                        int entityOffset = currentEntityOffset + 1;

                        entityOffset = entityOffset >= entities.length ? 0 : entityOffset;

                        setCurrentEntity(entityOffset);
                    }

                    break;

                case KeyEvent.VK_KP_UP:
                case KeyEvent.VK_UP:

                    // Raise setpoint for currently selected zone (if it is a zone)

                case KeyEvent.VK_KP_DOWN:
                case KeyEvent.VK_DOWN:

                    // Lower setpoint for currently selected zone (if it is a zone)

                    entities[currentEntityOffset].entityPanel.keyPressed(e);

                    break;

                default:

                    // Do nothing
                }

                default:

                    // Do nothing
            }

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No special handling
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No special handling
    }

    /**
     * Change the currently displayed entity to the one with the given offset.
     *
     * @param entityOffset Offset of the zone to display.
     */
    private void setCurrentEntity(int entityOffset) {

        entities[currentEntityOffset].entityCell.setSelected(false);
        entities[entityOffset].entityCell.setSelected(true);

        cardLayout.show(selectorPanel, "" + entityOffset);

        currentEntityOffset = entityOffset;
    }

    public synchronized void setSize(ScreenDescriptor screenDescriptor) {

        for (Entity entity : entities) {
            EntityPanel ep = entity.entityPanel;
            ep.setFontSize(screenDescriptor);
        }
    }
}
