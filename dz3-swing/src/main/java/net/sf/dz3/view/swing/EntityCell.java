package net.sf.dz3.view.swing;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JPanel;

public abstract class EntityCell<T> extends JPanel implements DataSink<T> {
    protected final transient Logger logger = LogManager.getLogger(getClass());
}
