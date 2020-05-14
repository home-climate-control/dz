package net.sf.dz3.view.swing;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.model.DamperController;
import net.sf.dz3.device.model.ThermostatController;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.TemperatureSensor;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.Connector;
import net.sf.dz3.view.ConnectorFactory;
import net.sf.dz3.view.swing.thermostat.ThermostatFactory;
import net.sf.dz3.view.swing.thermostat.ZonePanel;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Entry point into the user interface implemented with Swing.
 *
 * This object is supposed to be instantiated via Spring configuration file, with objects
 * that are supposed to be displayed being present in a set passed to the constructor.
 *
 * Valid object types are:
 *
 * - Sensor ({@link TemperatureSensor temperature}, humidity, pressure);
 * - {@link ThermostatController};
 * - {@link ZoneController};
 * - {@link Unit};
 * - {@link DamperController},
 * - {@link Scheduler}.
 *
 * All others will be ignored with a log message produced. Panel rendering is happening as a part of
 * {@link #show()}.
 *
 * {@code init-method="show"} attribute must be used in Spring bean definition, otherwise
 * the panel will not display.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class Console extends Connector<JComponent> {

    /**
     * Application main frame.
     *
     * Shown in {@link #show()}, destroyed again in {@link #hide()} in a way that allows it
     * to be completely rebuilt from scratch again.
     */
    private JFrame mainFrame;

    private ZonePanel zonePanel;

    /**
     * The scheduler, or {@code null} if one wasn't found.
     */
    private Scheduler scheduler;

    /**
     * Default constructor.
     *
     * This one will produce an empty console panel.
     */
    public Console() {
        this(new HashSet<Object>());
    }

    /**
     * Create an instance and fill it up with objects to display.
     *
     * @param initSet Objects to display.
     */
    public Console(Set<Object> initSet) {

        super(initSet);

        register(ThermostatModel.class, new ThermostatFactory());
    }

    /**
     * Create an instance and fill it up with objects to display,
     * using custom factory set.
     *
     * @param initSet Objects to display.
     * @param factorySet Set of {@link ComponentFactory} objects to use for component creation.
     */
    public Console(Set<Object> initSet, Set<ConnectorFactory<JComponent>> factorySet) {

        super(initSet, factorySet);
    }

    /**
     * Show the console.
     *
     * @deprecated Use {@link Connector#activate()} instead.
     */
    @Deprecated
    public synchronized void show() {
        logger.warn("use 'init-method=\"activate\"' instead of 'init-method=\"show\"'");
        activate();
    }

    @Override
    protected void activate2() {

        ThreadContext.push("activate2");

        try {

            createFrame();

            mainFrame.setSize(screenSizes[screenSizeOffset].displaySize);
            mainFrame.setVisible(true);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Figure out whether the scheduler was passed in as an argument, and assign
     * it to {@link #scheduler} if so.
     *
     * Ignore multiple scheduler instances and use only the first one.
     */
    private void findScheduler() {

        ThreadContext.push("findScheduler");

        try {

            for (Iterator<Object> i = getInitSet().iterator(); i.hasNext(); ) {

                Object initObject = i.next();

                if (initObject instanceof Scheduler) {

                    this.scheduler = (Scheduler) initObject;
                    logger.debug("found");
                    return;
                }
            }

            logger.warn("No scheduler was given, is this intentional?");

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Initialize the {@link #mainFrame} and stuff it with components,
     * but don't make it visible just yet.
     */
    private void createFrame() {

        ThreadContext.push("createFrame");

        try {

            mainFrame = new JFrame("DIY Zoning Console");
            mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            Container display = mainFrame.getContentPane();

            display.setBackground(ColorScheme.offMap.BACKGROUND);

            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints cs = new GridBagConstraints();

            display.setLayout(layout);

            zonePanel = new ZonePanel(getComponentMap());

            cs.fill = GridBagConstraints.BOTH;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = GridBagConstraints.REMAINDER;
            cs.gridheight = GridBagConstraints.REMAINDER;
            cs.weightx = 1;
            cs.weighty = 1;

            layout.setConstraints(zonePanel, cs);
            display.add(zonePanel);

            display.setFocusable(true);
            display.addKeyListener(new ResizeKeyListener());
            display.addKeyListener(zonePanel);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void deactivate2() {

        throw new UnsupportedOperationException("Not Implenented");
    }

    @JmxAttribute(description = "Is the console currently visible")
    public synchronized boolean isVisible() {
        return mainFrame != null;
    }

    /**
     * Show or hide the console.
     *
     * @param visible {@code true} if the console needs to be visible, otherwise {@code false}.
     */
    public synchronized void setVisible(boolean visible) {

        ThreadContext.push("setVisible");

        try {

            logger.info("requested visible={}", visible);

            if (visible && mainFrame != null) {

                logger.debug("Already visible");
                return;
            }

            if (!visible && mainFrame == null) {

                logger.debug("Already hidden");
                return;
            }

            if (visible) {
                activate();
            } else {
                deactivate();
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                Integer.toHexString(hashCode()),
                "Swing Console");
    }

    private static final String FONT_NAME = "Lucida Bright";

    private final Font font20 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 20);
    private final Font font24 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 24);
    @SuppressWarnings("unused")
    private final Font font30 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 30);
    private final Font font36 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 36);
    @SuppressWarnings("unused")
    private final Font fontBold36 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 36);
    private final Font fontBold48 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 48);
    private final Font fontBold72 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 72);
    private final Font fontBold96 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 96);
    private final Font fontBold120 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 120);
    private final Font fontBold144 = new Font(FONT_NAME, Font.ROMAN_BASELINE, 144);

    /**
     * Possible screen sizes.
     */
    private ScreenDescriptor[] screenSizes = {
            new ScreenDescriptor("QVGA", new Dimension(240, 320), fontBold72, fontBold48, font20),
            new ScreenDescriptor("WQVGA", new Dimension(240, 400), fontBold72, fontBold48, font20),
            new ScreenDescriptor("FWQVGA", new Dimension(240, 432), fontBold72, fontBold48, font20),
            new ScreenDescriptor("HVGA", new Dimension(320, 480), fontBold96, fontBold72, font24),
            new ScreenDescriptor("WVGA800", new Dimension(480, 800), fontBold144, fontBold120, font36),
            new ScreenDescriptor("WVGA854", new Dimension(480, 854), fontBold144, fontBold120, font36)
    };

    /**
     * Offset into {@link #screenSizes}, default is 3 (WVGA800).
     */
    private int screenSizeOffset = 4;

    private class ResizeKeyListener implements KeyListener {


        @Override
        public void keyPressed(KeyEvent e) {

            ThreadContext.push("keyPressed");

            try {

                logger.info(e.toString());

                switch (e.getKeyChar()) {

                case '-':

                    // Cycle display size to next bigger (and possibly roll over

                {
                    int sizeOffset = screenSizeOffset - 1;

                    sizeOffset = sizeOffset < 0 ? screenSizes.length - 1 : sizeOffset;
                    screenSizeOffset = sizeOffset;

                    setScreenSize(screenSizes[screenSizeOffset]);
                }

                break;

                case '+':

                    // Cycle display size to next smaller (and possibly roll under

                {
                    int sizeOffset = screenSizeOffset + 1;

                    sizeOffset = sizeOffset >= screenSizes.length ? 0 : sizeOffset;
                    screenSizeOffset = sizeOffset;

                    setScreenSize(screenSizes[screenSizeOffset]);
                }

                break;
                }

            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

            // Do nothing
        }

        @Override
        public void keyTyped(KeyEvent e) {

            // Do nothing
        }

        private void setScreenSize(ScreenDescriptor screenDescriptor) {

            ThreadContext.push("setScreenSize");

            try {

                logger.info("Setting screen size {} ({}x{})",
                        screenDescriptor.name,
                        screenDescriptor.displaySize.width,
                        screenDescriptor.displaySize.height);

                zonePanel.setSize(screenDescriptor);
                mainFrame.setSize(screenDescriptor.displaySize);
                mainFrame.invalidate();

            } finally {
                ThreadContext.pop();
            }
        }

    }

    @Override
    protected Map<String, Object> createContext() {

        Map<String, Object> context = new TreeMap<>();

        findScheduler();

        context.put("scheduler", scheduler);
        context.put("screen descriptor", screenSizes[screenSizeOffset]);

        return context;
    }
}
