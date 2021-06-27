package net.sf.dz3.view.swing.zone;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.controller.ProcessControllerStatus;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.PidControllerStatus;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneState;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityPanel;
import net.sf.dz3.view.swing.EntitySelectorPanel;
import net.sf.dz3.view.swing.ScreenDescriptor;
import net.sf.dz3.view.swing.TemperatureUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.time.Clock;
import java.util.Locale;

/**
 * Zone panel.
 *
 * Even though it implements {@link KeyListener}, it never request focus,
 * but gets event notifications from {@link EntitySelectorPanel} instead.
 * This is done in order not to fiddle with focus changes.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZonePanel extends EntityPanel implements KeyListener {

    private static final long serialVersionUID = 3420150515187693627L;
    private static final DecimalFormat numberFormat = new DecimalFormat("#0.0;-#0.0");

    private final transient Logger logger = LogManager.getLogger(getClass());

    private final transient ThermostatModel source;
    private final transient Scheduler scheduler;

    private final transient ThermostatListener thermostatListener = new ThermostatListener();
    private final transient PidControllerListener pidListener = new PidControllerListener();

    private static final String UNDEFINED = "--.-";

    private static final String VOTING = "VOTING";
    private static final String NOT_VOTING = "NOT VOTING";

    private static final String HOLD = "HOLD";
    private static final String ON_HOLD = "ON HOLD";

    private final JLabel currentLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);
    private final JLabel setpointLabel = new JLabel(UNDEFINED + "\u00b0", SwingConstants.RIGHT);
    private final JLabel votingLabel = new JLabel(VOTING, SwingConstants.RIGHT);
    private final JLabel holdLabel = new JLabel(HOLD, SwingConstants.RIGHT);
    private final JLabel periodLabel = new JLabel("", SwingConstants.LEFT);

    // 3 hours
    private final AbstractChart chart = new Chart2020(Clock.systemUTC(), 1000L * 60 * 60 * 3);

    private static final String NO_PERIOD = "(no period is active)";

    /**
     * Font to display the current temperature in Celsius.
     */
    private Font currentFontC;

    /**
     * Font to display the current temperature in Fahrenheit when the temperature is over 100F.
     */
    private Font currentFontF;

    /**
     * Font to display the setpoint with.
     */
    private Font setpointFont;

    private boolean needFahrenheit;

    /**
     * Setpoint change upon a keypress.
     *
     */
    private double setpointDelta = 0.1d;

    public ZonePanel(ThermostatModel source, ScreenDescriptor screenDescriptor, Scheduler scheduler, TemperatureUnit defaultUnit) {

        this.source = source;
        this.scheduler = scheduler;
        this.needFahrenheit = defaultUnit == TemperatureUnit.FAHRENHEIT;

        setFontSize(screenDescriptor);

        initGraphics();

        // This can only be done when everything else is done, to avoid bootstrapping problems

        source.addConsumer(thermostatListener);
        source.getController().addConsumer(pidListener);
    }

    private void initGraphics() {

        currentLabel.setFont(currentFontC);
        currentLabel.setToolTipText("Current temperature (Left/Right to change zone)");

        setpointLabel.setFont(setpointFont);
        setpointLabel.setToolTipText("Setpoint (Up/Down to change)");

        createLayout(source.getName(), chart);
    }

    @Override
    @SuppressWarnings("squid:S1199")
    protected JPanel createControls() {

        var controls = new JPanel();

        controls.setBackground(ColorScheme.offMap.background);
        controls.setOpaque(false);

        var layout = new GridBagLayout();
        var cs = new GridBagConstraints();

        controls.setLayout(layout);

        // VT: NOTE: squid:S1199 - SonarLint is not smart enough to realize that these
        // blocks are for readability

        {
            // Period label is on top left

            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 2;
            cs.fill = GridBagConstraints.HORIZONTAL;

            layout.setConstraints(periodLabel, cs);
            controls.add(periodLabel);

            periodLabel.setForeground(Color.GRAY);
        }

        {
            // Current label takes all available space on the left
            // and expands until it meets the setpoint and voting/hold labels

            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.gridx = 0;
            cs.gridy++;
            cs.gridwidth = 1;
            cs.gridheight = 3;
            cs.weightx = 1;
            cs.weighty = 0;

            layout.setConstraints(currentLabel, cs);
            controls.add(currentLabel);
        }

        {
            // Setpoint, hold and voting buttons form a group to the right of the current
            // temperature reading, and take the rest of the row

            cs.fill = GridBagConstraints.VERTICAL;
            cs.gridx++;

            cs.gridheight = 1;
            cs.gridwidth = GridBagConstraints.REMAINDER;

            cs.weightx = 0;

            {
                // Setpoint label takes the rest of the space on the right in the top row

                // It takes more space than voting and hold labels

                cs.weighty = 1;

                layout.setConstraints(setpointLabel, cs);
                controls.add(setpointLabel);
            }

            {
                // Hold label is underneath the setpoint label

                cs.gridy++;

                cs.weighty = 0;

                layout.setConstraints(holdLabel, cs);
                controls.add(holdLabel);
            }

            {
                // Voting label is underneath the hold label

                cs.gridy++;

                layout.setConstraints(votingLabel, cs);
                controls.add(votingLabel);
            }
        }

        return controls;
    }

    @Override
    public void keyPressed(KeyEvent e) {

        ThreadContext.push("keyPressed");

        try {

            logger.info("{}", e::toString);

            switch (e.getKeyChar()) {

            case 'c':
            case 'C':
            case 'f':
            case 'F':

                needFahrenheit = !needFahrenheit;
                refresh();

                logger.info("Displaying temperature in {}", (needFahrenheit ? "Fahrenheit" : "Celsius"));

                break;

            case 'h':
            case 'H':

                // Toggle hold status

                source.setOnHold(!source.isOnHold());
                refresh();

                logger.info("Hold status for {} is now {}", source.getName(), source.isOnHold());

                break;

            case 'v':
            case 'V':

                // Toggle voting status

                source.setVoting(!source.isVoting());
                refresh();

                logger.info("Voting status for {} is now {}", source.getName(), source.isVoting());

                break;

            case 'o':
            case 'O':

                // Toggle off status

                source.setOn(!source.isOn());
                refresh();

                logger.info("On status for {} is now {}", source.getName(), source.isOn());

                break;

            case 's':
            case 'S':

                // Go back to schedule

                // Implies taking the zone off hold
                source.setOnHold(false);
                activateSchedule();
                refresh();

                break;

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

                source.setDumpPriority(e.getKeyChar() - '0');
                refresh();

                logger.info("Dump priority for{} is now {}", source.getName(), source.getDumpPriority());

                break;

            case KeyEvent.CHAR_UNDEFINED:

                switch (e.getKeyCode()) {

                case KeyEvent.VK_KP_UP:
                case KeyEvent.VK_UP:

                raiseSetpoint();
                break;

                case KeyEvent.VK_KP_DOWN:
                case KeyEvent.VK_DOWN:

                lowerSetpoint();
                break;

                default:
                    // Do nothing
                }
                break;

            default:
                // Do nothing
            }

        } finally {
            ThreadContext.pop();
        }
    }

    private void raiseSetpoint() {

        // If the zone is off, the setpoint is not displayed, hence we won't accept changes to avoid
        // unpleasant surprises later

        if (!source.isOn()) {
            logger.info("{} zone is off, not changing setpoint", source.getName());
            return;
        }

        var controller = source.getController();
        var setpoint = getDisplayValue(controller.getSetpoint());

        setpoint += setpointDelta;
        setpoint = getSIValue(Double.parseDouble(numberFormat.format(setpoint)));

        if (setpoint <= ThermostatModel.SETPOINT_MAX) {
            controller.setSetpoint(setpoint);
        } else {
            logger.warn("Setpoint change to {} denied, over high limit of {}", setpoint, ThermostatModel.SETPOINT_MAX);
        }
        refresh();
    }

    private void lowerSetpoint() {

        // If the zone is off, the setpoint is not displayed, hence we won't accept changes to avoid
        // unpleasant surprises later

        if (!source.isOn()) {
            logger.info("{} zone is off, not changing setpoint", source.getName());
            return;
        }

        var controller = source.getController();
        var setpoint = getDisplayValue(controller.getSetpoint());

        setpoint -= setpointDelta;
        setpoint = getSIValue(Double.parseDouble(numberFormat.format(setpoint)));

        if (setpoint >= ThermostatModel.SETPOINT_MIN) {
            controller.setSetpoint(setpoint);
        } else {
            logger.warn("Setpoint change to {} denied, under low limit of {}", setpoint, ThermostatModel.SETPOINT_MIN);
        }

        refresh();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No special handling
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No special handling
    }

    private void activateSchedule() {

        ThreadContext.push("activateSchedule");

        try {

            if (scheduler == null) {
                logger.warn("No scheduler was provided, ignored");
                return;
            }

            var period = scheduler.getCurrentPeriod(source);
            var status = scheduler.getCurrentStatus(source);

            if (period == null) {
                logger.info("No period is active, ignored");
                return;
            }

            if (status == null) {
                logger.error("Period is resolved as {}, but no status???", period);
                return;
            }

            if (status.equals(source.getStatus())) {
                logger.info("Already at period settings");
                return;
            }

            logger.info("Activating period: {}", period);
            source.set(status);

        } finally {
            ThreadContext.pop();
        }
    }

    public synchronized void refresh() {
        thermostatListener.refresh();
        pidListener.refresh();
    }

    private HvacMode getMode() {
        return ((AbstractPidController) source.getController()).getP() > 0 ? HvacMode.COOLING : HvacMode.HEATING;
    }

    private class ThermostatListener implements DataSink<ThermostatSignal> {

        private DataSample<ThermostatSignal> signal;

        @Override
        public void consume(DataSample<ThermostatSignal> signal) {
            this.signal = signal;
            refresh();
        }

        public void refresh() {

            DataSample<Double> pv = source.getController().getProcessVariable();
            ThermostatSignal sample = signal == null ? null : signal.sample;

            final String displayTemperature;

            if (pv == null) {

                displayTemperature = UNDEFINED;

            } else {

                // VT: This block is embarrassing - wonder what I was smoking back when I wrote
                // it. The net result is that values are written into the chart in *currently
                // selected* unit, resulting in a miniature clone of Mars Climate Orbiter
                // disaster right here in case someone decides to switch between Celsius and
                // Fahrenheit and then back.

                // The right way to deal with this would be to use SI units everywhere except
                // final display.

                var currentTemperature = getDisplayValue(pv.sample);
                var currentSetpoint = getDisplayValue(source.getSetpoint());

                displayTemperature = (sample == null || sample.demand.isError()) ? UNDEFINED : String.format(Locale.getDefault(), "%.1f", currentTemperature);

                var tint = new TintedValueAndSetpoint(currentTemperature, source.getControlSignal() * 2, sample != null && sample.calling, currentSetpoint);
                chart.consume(new DataSample<>(pidListener.signal.timestamp, "temp", "temp", tint, null));
            }

            var fg = ColorScheme.getScheme(getMode()).setpoint;
            var font = needFahrenheit  && displayTemperature.length() > 4 ? currentFontF : currentFontC;

            currentLabel.setFont(font);
            currentLabel.setText(displayTemperature);
            currentLabel.setForeground(fg);
            setpointLabel.setForeground(fg);

            votingLabel.setText(source.isVoting() ? VOTING : NOT_VOTING);
            holdLabel.setText(source.isOnHold() ? ON_HOLD : HOLD);

            votingLabel.setForeground(source.isVoting() ? ColorScheme.getScheme(getMode()).noticeDefault : ColorScheme.getScheme(getMode()).noticeActive);
            holdLabel.setForeground(source.isOnHold() ? ColorScheme.getScheme(getMode()).noticeActive : ColorScheme.getScheme(getMode()).noticeDefault);

            renderPeriod();

            repaint();
        }

        public void renderPeriod() {

            if (scheduler == null) {
                periodLabel.setText(NO_PERIOD);
                return;
            }

            var period = scheduler.getCurrentPeriod(source);

            if (period == null) {
                periodLabel.setText(NO_PERIOD);
                return;
            }

            var sb = new StringBuilder();
            var status = scheduler.getCurrentStatus(source);

            if (status == null) {

                logger.warn("Period is resolved as {}, but no status???", period);

            } else {

                if (!status.equals(source.getStatus())) {

                    sb.append("* ");
                }
            }

            sb.append(period.name);

            periodLabel.setText(sb.toString());
        }
    }

    private class PidControllerListener implements DataSink<ProcessControllerStatus> {

        private DataSample<PidControllerStatus> signal;

        @Override
        public void consume(DataSample<ProcessControllerStatus> signal) {

            var sample = (PidControllerStatus) signal.sample;

            this.signal = new DataSample<>(signal.timestamp, signal.sourceName, signal.signature, sample, signal.error);

            refresh();
        }

        public void refresh() {

            String label;

            if (this.signal == null) {

                label = UNDEFINED + "\u00b0";

            } else {

                var setpoint = getDisplayValue(this.signal.sample.setpoint);

                label = source.isOn() ? String.format(Locale.getDefault(), "%.1f\u00b0", setpoint) : "OFF";
            }

            var fg = ColorScheme.getScheme(getMode()).setpoint;

            setpointLabel.setText(label);
            setpointLabel.setForeground(fg);

            // The signal path is such that this code will always be executed right before ThermostatListener#refresh
            // which also calls repaint(), hence this call is redundant.

            // repaint(); // NOSONAR Retained for documentation purposes
        }
    }

    @Override
    public synchronized void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (pidListener.signal == null || thermostatListener.signal == null) {
            // Nothing to do yet
            return;
        }

        var g2d = (Graphics2D) g;
        var d = getSize();

        var signal = thermostatListener.signal.sample.demand.sample;
        var mode = getMode();
        var state = resolveState(thermostatListener);
        var boundary = new Rectangle(0, 0, d.width, d.height);

        switch (state) {

        case CALLING:
        case ERROR:
        case OFF:

            BackgroundRenderer.drawBottom(state, mode, signal, g2d, boundary, true);
            break;

        case HAPPY:

            BackgroundRenderer.drawTop(mode, signal, g2d, boundary);
            break;
        }
    }

    private ZoneState resolveState(ThermostatListener source) {

        if ( source.signal == null || source.signal.sample.demand.isError()) {
            return ZoneState.ERROR;
        }

        if (!source.signal.sample.enabled) {
            return ZoneState.OFF;
        }

        return source.signal.sample.calling ? ZoneState.CALLING : ZoneState.HAPPY;
    }

    @Override
    public void setFontSize(ScreenDescriptor screenDescriptor) {

        this.currentFontC = screenDescriptor.fontCurrentTemperatureC;
        this.currentFontF = screenDescriptor.fontCurrentTemperatureF;
        this.setpointFont = screenDescriptor.fontSetpoint;
    }

    /**
     * Convert SI value into display value depending on whether the display is
     * currently in {@link #needFahrenheit Fahrenheit}.
     *
     * @param value Value to possibly convert.
     * @return Display value.
     */
    private double getDisplayValue(double value) {
        return needFahrenheit ? (value * 9) / 5d + 32: value;
    }

    /**
     * Convert display value into SI value depending on whether the display is
     * currently in {@link #needFahrenheit Fahrenheit}.
     *
     * @param value Value to possibly convert.
     * @return SI value.
     */
    private double getSIValue(double value) {
        return needFahrenheit ? (value - 32) * (5d / 9d) : value;
    }

    public double getSetpointDelta() {
        return setpointDelta;
    }

    public void setSetpointDelta(double setpointDelta) {
        this.setpointDelta = setpointDelta;
    }
}
