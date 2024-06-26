package net.sf.dz3r.view.swing;

import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.runtime.config.model.MeasurementUnits;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;
import java.util.Set;

/**
 * Entry point into the user interface implemented with Swing.
 *
 * This object is supposed to be instantiated via Spring configuration file, with objects
 * that are supposed to be displayed being present in a set passed to the constructor.
 *
 * Valid object types are:
 *
 * - {@link UnitDirector}
 * - {@link Signal} flux
 * - Scheduler
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ReactiveConsole {

    private final Logger logger = LogManager.getLogger();

    private final Config config;

    private JFrame mainFrame;

    private EntitySelectorPanel entitySelectorPanel;

    /**
     * Create an instance and fill it up with objects to display.
     *
     * @param instance HCC instance name to display on the UI title bar.
     * @param directors Directors to include into this console.
     * @param sensors Sensors to include information panels for into this console.
     * @param units Measurement units.
     */
    public ReactiveConsole(
            String instance,
            Set<UnitDirector> directors,
            Map<String, Flux<Signal<Double, Void>>> sensors,
            InstrumentCluster ic,
            MeasurementUnits units) {

        this.config = new Config(instance, directors, sensors, ic, units);

        Flux.just(config)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::start)
                .subscribe();
    }

    private void start(Config config) {

        Marker m = new Marker("ReactiveConsole#start", Level.INFO);
        ThreadContext.push("start");

        try {

            if (config.directors.isEmpty() && config.sensors.isEmpty()) {
                logger.warn("empty director and sensor sets, not creating mainFrame, what's the point?");
                return;
            }

            createFrame();

            mainFrame.setSize(screenSizes[screenSizeOffset].displaySize);
            mainFrame.setVisible(true);

        } finally {
            m.close();
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

            mainFrame = new JFrame("Home Climate Control: " + config.instance);
            mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            var display = mainFrame.getContentPane();

            display.setBackground(ColorScheme.offMap.background);

            var layout = new GridBagLayout();
            var cs = new GridBagConstraints();

            display.setLayout(layout);

            entitySelectorPanel = new EntitySelectorPanel(config, screenSizes[screenSizeOffset]);

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
                    case '-' -> reduce();
                    case '+' -> enlarge();
                    default -> {
                        // Not our key, do nothing
                    }
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

    public record Config(
            String instance,
            Set<UnitDirector> directors,
            Map<String, Flux<Signal<Double, Void>>> sensors,
            InstrumentCluster ic,
            MeasurementUnits measurementUnits
    ) {

    }
}
