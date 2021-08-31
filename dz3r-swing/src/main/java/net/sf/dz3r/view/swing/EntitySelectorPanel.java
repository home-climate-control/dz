package net.sf.dz3r.view.swing;

import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class EntitySelectorPanel extends JPanel implements KeyListener {

    private int currentEntityOffset = 0;
    private final transient CellAndPanel[] entities = null;

    /**
     * Panel to display {@link EntityPanel} instances.
     */
    private final JPanel selectorPanel = new JPanel();

    /**
     * Layout to control which entity is showing.
     */
    private final CardLayout cardLayout = new CardLayout();

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

    public synchronized void setSize(ScreenDescriptor screenDescriptor) {

        for (CellAndPanel entity : entities) {
            entity.panel.setFontSize(screenDescriptor);
        }
    }
}
