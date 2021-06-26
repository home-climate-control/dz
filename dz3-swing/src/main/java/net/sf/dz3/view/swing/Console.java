package net.sf.dz3.view.swing;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.model.DamperController;
import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.device.model.ThermostatController;
import net.sf.dz3.device.model.Unit;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.device.sensor.TemperatureSensor;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.Connector;
import net.sf.dz3.view.ConnectorFactory;
import net.sf.dz3.view.swing.sensor.SensorFactory;
import net.sf.dz3.view.swing.thermostat.ThermostatFactory;
import net.sf.dz3.view.swing.unit.UnitFactory;
import org.apache.logging.log4j.ThreadContext;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
 * {@link #activate()}.
 *
 * {@code init-method="activate"} attribute must be used in Spring bean definition, otherwise
 * the panel will not display.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class Console extends Connector<JComponent> {

    private final TemperatureUnit defaultUnit;

    /**
     * Application main frame.
     *
     * Shown in {@link #activate()}, destroyed again in {@link #deactivate()} in a way that allows it
     * to be completely rebuilt from scratch again.
     */
    private JFrame mainFrame;

    private EntitySelectorPanel entitySelectorPanel;

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
        this(new HashSet<>(), "C");
    }

    /**
     * Create an instance and fill it up with objects to display.
     *
     * @param initSet Objects to display.
     */
    public Console(Set<Object> initSet) {
        this(initSet, "C");
    }

    /**
     * Create an instance and fill it up with objects to display.
     *
     * @param initSet Objects to display.
     * @param unit Initial temperature unit to display. Can be either {@code "C.*"} for Celsius, or {@code "F.*"} for Fahrenheit.
     */
    public Console(Set<Object> initSet, String unit) {

        super(initSet);
        this.defaultUnit = TemperatureUnit.resolve(unit);

        register(AnalogSensor.class, new SensorFactory());
        register(ThermostatModel.class, new ThermostatFactory(this.defaultUnit));
        register(RuntimePredictor.class, new UnitFactory());
    }

    /**
     * Create an instance and fill it up with objects to display,
     * using custom factory set.
     *
     * @param initSet Objects to display.
     * @param factorySet Set of {@link ComponentFactory} objects to use for component creation.
     */
    public Console(Set<Object> initSet, Set<ConnectorFactory<JComponent>> factorySet) {
        this(initSet, factorySet, "C");
    }

    /**
     * Create an instance and fill it up with objects to display,
     * using custom factory set.
     *
     * @param initSet Objects to display.
     * @param factorySet Set of {@link ComponentFactory} objects to use for component creation.
     * @param unit Initial temperature unit to display. Can be either {@code "C.*"} for Celsius, or {@code "F.*"} for Fahrenheit.
     */
    public Console(Set<Object> initSet, Set<ConnectorFactory<JComponent>> factorySet, String unit) {
        super(initSet, factorySet);
        this.defaultUnit = TemperatureUnit.resolve(unit);
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

            for (Object initObject : getInitSet()) {

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

            var display = mainFrame.getContentPane();

            display.setBackground(ColorScheme.offMap.background);

            var layout = new GridBagLayout();
            var cs = new GridBagConstraints();

            display.setLayout(layout);

            entitySelectorPanel = new EntitySelectorPanel(getComponentMap());

            cs.fill = GridBagConstraints.BOTH;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = GridBagConstraints.REMAINDER;
            cs.gridheight = GridBagConstraints.REMAINDER;
            cs.weightx = 1;
            cs.weighty = 1;

            layout.setConstraints(entitySelectorPanel, cs);
            display.add(entitySelectorPanel);

            display.setFocusable(true);
            display.addKeyListener(new ResizeKeyListener());
            display.addKeyListener(entitySelectorPanel);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void deactivate2() {
        throw new UnsupportedOperationException("Not Implemented");
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

    private final Font font20 = new Font(FONT_NAME, Font.PLAIN, 20);
    private final Font font24 = new Font(FONT_NAME, Font.PLAIN, 24);
    @SuppressWarnings("unused")
    private final Font font30 = new Font(FONT_NAME, Font.PLAIN, 30);
    private final Font font36 = new Font(FONT_NAME, Font.PLAIN, 36);
    @SuppressWarnings("unused")
    private final Font fontBold36 = new Font(FONT_NAME, Font.PLAIN, 36);
    private final Font fontBold48 = new Font(FONT_NAME, Font.PLAIN, 48);
    private final Font fontBold72 = new Font(FONT_NAME, Font.PLAIN, 72);
    private final Font fontBold96 = new Font(FONT_NAME, Font.PLAIN, 96);
    private final Font fontBold120 = new Font(FONT_NAME, Font.PLAIN, 120);
    private final Font fontBold144 = new Font(FONT_NAME, Font.PLAIN, 144);

    /**
     * Possible screen sizes.
     */
    private final ScreenDescriptor[] screenSizes = {
            new ScreenDescriptor("QVGA", new Dimension(240, 320), fontBold72, fontBold48, font20),
            new ScreenDescriptor("WQVGA", new Dimension(240, 400), fontBold72, fontBold48, font20),
            new ScreenDescriptor("FWQVGA", new Dimension(240, 432), fontBold72, fontBold48, font20),
            new ScreenDescriptor("HVGA", new Dimension(320, 480), fontBold96, fontBold72, font24),
            new ScreenDescriptor("WVGA800", new Dimension(480, 800), fontBold144, fontBold120, font36),
            new ScreenDescriptor("WVGA854", new Dimension(480, 854), fontBold144, fontBold120, font36)
    };

    /**
     * Offset into {@link #screenSizes}, default is 5 (WVGA854).
     */
    private int screenSizeOffset = 5;

    private class ResizeKeyListener implements KeyListener {


        @Override
        public void keyPressed(KeyEvent e) {

            ThreadContext.push("keyPressed");

            try {

                switch (e.getKeyChar()) {

                case '-':

                    reduce();
                    break;

                case '+':

                    enlarge();
                    break;

                default:

                    // Not our key, do nothing
                }

            } finally {
                ThreadContext.pop();
            }
        }

        /**
         * Cycle display size to next smaller (and possibly roll under).
         */
        private void reduce() {
            int sizeOffset = screenSizeOffset - 1;

            sizeOffset = sizeOffset < 0 ? screenSizes.length - 1 : sizeOffset;
            screenSizeOffset = sizeOffset;

            setScreenSize(screenSizes[screenSizeOffset]);
        }

        /**
         * Cycle display size to next bigger (and possibly roll over).
         */
        private void enlarge() {
            int sizeOffset = screenSizeOffset + 1;

            sizeOffset = sizeOffset >= screenSizes.length ? 0 : sizeOffset;
            screenSizeOffset = sizeOffset;

            setScreenSize(screenSizes[screenSizeOffset]);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // No special handling
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // No special handling
        }

        private void setScreenSize(ScreenDescriptor screenDescriptor) {

            ThreadContext.push("setScreenSize");

            try {

                logger.info("Setting {} ({}x{})",
                        screenDescriptor.name,
                        screenDescriptor.displaySize.width,
                        screenDescriptor.displaySize.height);

                entitySelectorPanel.setSize(screenDescriptor);
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
