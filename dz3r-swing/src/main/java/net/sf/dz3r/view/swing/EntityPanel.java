package net.sf.dz3r.view.swing;

import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyListener;

public abstract class EntityPanel extends JPanel implements KeyListener {
    protected abstract void createControls(JPanel controls, GridBagLayout layout, GridBagConstraints cs);
    public abstract void setFontSize(ScreenDescriptor screenDescriptor);
}
