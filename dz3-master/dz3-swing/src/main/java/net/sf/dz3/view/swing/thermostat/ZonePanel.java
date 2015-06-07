package net.sf.dz3.view.swing.thermostat;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.sf.dz3.device.model.Thermostat;
import net.sf.dz3.view.swing.ScreenDescriptor;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * Panel that contains all {@link ThermostatPanel} instances (and all {@code SensorPanel} instances
 * in a {@link CardLayout}, and an indicator bar that displays abbreviated status for all zones.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class ZonePanel extends JPanel implements KeyListener {
    
    private static final long serialVersionUID = 6400746493551083129L;
    
    private Logger logger = Logger.getLogger(getClass());

    private int currentZoneOffset = 0;
    private final Zone[] zones;
    
    /**
     * Panel to display bars for all zones.
     */
    private final JPanel zoneBar = new JPanel();
    
    /**
     * Panel to display {@link ThermostatPanel} instances.
     */
    private final JPanel zonePanel = new JPanel();
    
    /**
     * Layout to control which zone is showing.
     */
    private final CardLayout cardLayout = new CardLayout();
    
    public ZonePanel(Map<Object, JComponent> componentMap) {
        
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cs = new GridBagConstraints();
        
        this.setLayout(layout);
        
        // Zone bar spans all the horizontal space available (as many cells as there are zones),
        // but the height is limited
        
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        cs.gridheight = 1;
        cs.weightx = 1;
        cs.weighty = 0;
        
        layout.setConstraints(zoneBar, cs);
        this.add(zoneBar);
        
        // Zone panel is right below the zone bar,
        // same constraints work for it
        cs.gridy++;
        
        layout.setConstraints(zonePanel, cs);
        this.add(zonePanel);
        
        SortedMap<Thermostat, JComponent> thermostatMap = new TreeMap<Thermostat, JComponent>();

        for (Iterator<Entry<Object, JComponent>> i = componentMap.entrySet().iterator(); i.hasNext(); ) {
            
            Entry<Object, JComponent> entry = i.next();
            Object dataSource = entry.getKey();
            JComponent panel = entry.getValue();
            
            // VT: FIXME: Extend this to all sensors
            
            if (dataSource instanceof Thermostat) {
                
                thermostatMap.put((Thermostat)dataSource, panel);
            }
        }
        
        zones = new Zone[thermostatMap.size()];
        
        zoneBar.setLayout(new GridLayout(1, thermostatMap.size()));
        zonePanel.setLayout(cardLayout);
        
        int offset = 0;
        for (Iterator<Thermostat> i = thermostatMap.keySet().iterator(); i.hasNext(); ) {
            
            Thermostat ts = i.next();
            JComponent panel = componentMap.get(ts);
            ZoneCell cell = new ZoneCell(ts);
            
            Zone zone = new Zone(cell, (ThermostatPanel) panel);

            zoneBar.add(cell);
            zonePanel.add(panel, "" + offset);
            
            zones[offset++] = zone;
        }
        
        setCurrentZone(0);
    }
    
    private class Zone {
        
        public final ZoneCell zoneCell;
        public final ThermostatPanel thermostatPanel;
        
        public Zone(ZoneCell zoneCell, ThermostatPanel thermostatPanel) {
            
            this.zoneCell = zoneCell;
            this.thermostatPanel = thermostatPanel;
        }
    }

    /**
     * Handle arrow right and left (change zone).
     */
    @Override
    public synchronized void keyPressed(KeyEvent e) {
        
        NDC.push("keyPressed");
        
        try {
            
            logger.info(e.toString());
            
            switch (e.getKeyChar()) {
            
            case 'c':
            case 'C':
            case 'f':
            case 'F':
                
                // Toggle between Celsius and Fahrenheit
                
                {
                    // This must work for all zones
                    for (int offset = 0; offset < zones.length; offset++) {

                        zones[offset].thermostatPanel.keyPressed(e);
                    }
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
                
                zones[currentZoneOffset].thermostatPanel.keyPressed(e);
                break;
                
            case KeyEvent.CHAR_UNDEFINED:
            
                switch (e.getKeyCode()) {

                case KeyEvent.VK_KP_LEFT:
                case KeyEvent.VK_LEFT:
                    
                    // Cycle displayed zone to the left

                    {
                        int zoneOffset = currentZoneOffset - 1;
        
                        zoneOffset = zoneOffset < 0 ? zones.length - 1 : zoneOffset;
        
                        setCurrentZone(zoneOffset);
                    }

                    break;

                case KeyEvent.VK_KP_RIGHT:
                case KeyEvent.VK_RIGHT:

                    // Cycle displayed zone to the right
                    
                    {
                        int zoneOffset = currentZoneOffset + 1;
        
                        zoneOffset = zoneOffset >= zones.length ? 0 : zoneOffset;
        
                        setCurrentZone(zoneOffset);
                    }
                    
                    break;

                case KeyEvent.VK_KP_UP:
                case KeyEvent.VK_UP:

                    // Raise setpoint for currently selected zone
                    
                case KeyEvent.VK_KP_DOWN:
                case KeyEvent.VK_DOWN:

                    // Lower setpoint for currently selected zone
                    
                    {
                        zones[currentZoneOffset].thermostatPanel.keyPressed(e);
                    }

                    break;
                }
            }
            
        } finally {
            NDC.pop();
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
     * Change the currently displayed zone to the one with the given offset.
     * 
     * @param zoneOffset Offset of the zone to display.
     */
    private void setCurrentZone(int zoneOffset) {
        
        zones[currentZoneOffset].zoneCell.setSelected(false);
        zones[zoneOffset].zoneCell.setSelected(true);
        
        cardLayout.show(zonePanel, "" + zoneOffset);
        
        currentZoneOffset = zoneOffset;
    }

    public synchronized void setSize(ScreenDescriptor screenDescriptor) {
        
        for (int offset = 0; offset < zones.length; offset++) {
            
            ThermostatPanel tp = zones[offset].thermostatPanel;
            
            tp.setFontSize(screenDescriptor);
        }
    }
}
