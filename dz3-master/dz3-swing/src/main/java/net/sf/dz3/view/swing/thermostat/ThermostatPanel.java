package net.sf.dz3.view.swing.thermostat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.util.Formatter;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.controller.ProcessController;
import net.sf.dz3.controller.ProcessControllerStatus;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.controller.pid.PidControllerStatus;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneState;
import net.sf.dz3.device.model.ZoneStatus;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.scheduler.Period;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.ScreenDescriptor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
 * Thermostat panel.
 * 
 * Even though it implements {@link KeyListener}, it never request focus,
 * but gets event notifications from {@link ZonePanel} instead.
 * This is done in order not to fiddle with focus changes.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class ThermostatPanel extends JPanel implements KeyListener {

    /**
     * Setpoint change upon a keypress.
     * 
     * VT: FIXME: This must be made configurable.
     */
    public static final double SETPOINT_DELTA = 0.1d;

    private static final long serialVersionUID = 3420150515187693627L;
    private static final DecimalFormat numberFormat = new DecimalFormat("#0.0;-#0.0");

    private Logger logger = LogManager.getLogger(getClass());

    private final ThermostatModel source;
    private final Scheduler scheduler;

    private final ThermostatListener thermostatListener = new ThermostatListener();
    private final PidControllerListener pidListener = new PidControllerListener();

    private static final String UNDEFINED = "--.-";

    private static final String VOTING = "VOTING";
    private static final String NOT_VOTING = "NOT VOTING";

    private static final String HOLD = "HOLD";
    private static final String ON_HOLD = "ON HOLD";

    private final JLabel currentLabel = new JLabel(UNDEFINED, JLabel.RIGHT);
    private final JLabel setpointLabel = new JLabel(UNDEFINED + "\u00b0", JLabel.RIGHT);
    private final JLabel votingLabel = new JLabel(VOTING, JLabel.RIGHT);
    private final JLabel holdLabel = new JLabel(HOLD, JLabel.RIGHT);
    private final JLabel periodLabel = new JLabel("", JLabel.LEFT);

    // 3 hours
    private final AbstractChart chart = new FasterChart(1000 * 60 * 60 * 3);

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

    private boolean needFahrenheit = false;

    public ThermostatPanel(ThermostatModel source, ScreenDescriptor screenDescriptor, Scheduler scheduler) {

        this.source = source;
        this.scheduler = scheduler;

        setFontSize(screenDescriptor); 

        initGraphics();

        // This can only be done when everything else is done, to avoid bootstrapping problems

        source.addConsumer(thermostatListener);
        source.getController().addConsumer(pidListener);
    }

    private void initGraphics() {

        setBackground(ColorScheme.offMap.BACKGROUND);

        currentLabel.setFont(currentFontC);
        currentLabel.setToolTipText("Current temperature (Left/Right to change zone)");

        setpointLabel.setFont(setpointFont);
        setpointLabel.setToolTipText("Setpoint (Up/Down to change)");

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cs = new GridBagConstraints();

        this.setLayout(layout);

        {
            // Controls take the upper quarter of the display

            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = GridBagConstraints.REMAINDER;
            cs.gridheight = 1;
            cs.weightx = 1;
            cs.weighty = 0;

            JPanel controls = createControls();

            layout.setConstraints(controls, cs);
            this.add(controls);
        }

        {
            cs.gridy++;
            cs.gridheight = 1;
            cs.weighty = 1;
            cs.fill = GridBagConstraints.BOTH;

            layout.setConstraints(chart, cs);
            this.add(chart);

            chart.setPreferredSize(getPreferredSize());
            Color bg = ColorScheme.offMap.BACKGROUND;
            Color chartBg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0x00);
            chart.setBackground(chartBg);
        }

        // Really dirty, but really quick

        TitledBorder template = BorderFactory.createTitledBorder(source.getName());
        TitledBorder border = BorderFactory.createTitledBorder(
                getBorder(),
                source.getName(),
                template.getTitleJustification(),
                template.getTitlePosition(),
                template.getTitleFont(),
                Color.WHITE);

        this.setBorder(border);

        //        nonVotingLabel.setFont(setpointFont);
        //        holdLabel.setFont(setpointFont);

        {
            // Borders are to debug the layout, pain in the butt

//            currentLabel.setBorder(BorderFactory.createEtchedBorder());
//            setpointLabel.setBorder(BorderFactory.createEtchedBorder());
//            votingLabel.setBorder(BorderFactory.createEtchedBorder());
//            holdLabel.setBorder(BorderFactory.createEtchedBorder());
//            periodLabel.setBorder(BorderFactory.createEtchedBorder());
//            chart.setBorder(javax.swing.BorderFactory.createTitledBorder("Chart"));
        }
    }

    private JPanel createControls() {

        JPanel controls = new JPanel();

        controls.setBackground(ColorScheme.offMap.BACKGROUND);
        controls.setOpaque(false);

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cs = new GridBagConstraints();

        controls.setLayout(layout);

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

            logger.info(e.toString());

            switch (e.getKeyChar()) {

            case 'c':
            case 'C':
            case 'f':
            case 'F':

                needFahrenheit = !needFahrenheit;
                refresh();

                logger.info("Displaying temperature in " + (needFahrenheit ? "Fahrenheit" : "Celsius"));

                break;

            case 'h':
            case 'H':

                // Toggle hold status

                source.setOnHold(!source.isOnHold());
                refresh();

                logger.info("Hold status for " + source.getName() + " is now " + source.isOnHold());

                break;

            case 'v':
            case 'V':

                // Toggle voting status

                source.setVoting(!source.isVoting());
                refresh();

                logger.info("Voting status for " + source.getName() + " is now " + source.isVoting());

                break;

            case 'o':
            case 'O':

                // Toggle off status

                source.setOn(!source.isOn());
                refresh();

                logger.info("On status for " + source.getName() + " is now " + source.isOn());

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

                logger.info("Dump priority for " + source.getName() + " is now " + source.getDumpPriority());

                break;

            case KeyEvent.CHAR_UNDEFINED:

                switch (e.getKeyCode()) {

                case KeyEvent.VK_KP_UP:
                case KeyEvent.VK_UP:

                {

                    // If the zone is off, the setpoint is not displayed, hence we won't accept changes to avoid
                    // unpleasant surprises later

                    if (!source.isOn()) {
                        logger.info(source.getName() + " zone is off, not changing setpoint");
                        break;
                    }

                    ProcessController controller = source.getController(); 
                    double setpoint = controller.getSetpoint();

                    setpoint += needFahrenheit ? SETPOINT_DELTA / 1.8: SETPOINT_DELTA;
                    setpoint = Double.parseDouble(numberFormat.format(setpoint));

                    if (setpoint <= ThermostatModel.SETPOINT_MAX) {

                        controller.setSetpoint(setpoint);
                    } else {
                        logger.warn("Setpoint change to " + setpoint + " denied, over high limit");
                    }
                }

                refresh();
                break;

                case KeyEvent.VK_KP_DOWN:
                case KeyEvent.VK_DOWN:

                {
                    // If the zone is off, the setpoint is not displayed, hence we won't accept changes to avoid
                    // unpleasant surprises later

                    if (!source.isOn()) {
                        logger.info(source.getName() + " zone is off, not changing setpoint");
                        break;
                    }

                    ProcessController controller = source.getController(); 
                    double setpoint = controller.getSetpoint();

                    setpoint -= needFahrenheit ? SETPOINT_DELTA / 1.8: SETPOINT_DELTA;
                    setpoint = Double.parseDouble(numberFormat.format(setpoint));

                    if (setpoint >= ThermostatModel.SETPOINT_MIN) {

                        controller.setSetpoint(setpoint);
                    } else {
                        logger.warn("Setpoint change to " + setpoint + " denied, under low limit");
                    }
                }

                refresh();
                break;
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

            Period p = scheduler.getCurrentPeriod(source);
            ZoneStatus status = scheduler.getCurrentStatus(source);

            if (p == null) {

                logger.info("No period is active, ignored");
                return;
            }

            if (status == null) {

                logger.error("Period is resolved as " + p + ", but no status???");
                return;
            }

            if (status.equals(source.getStatus())) {

                logger.info("Already at period settings");
                return;
            }

            logger.info("Activating period: " + p);
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

        public DataSample<ThermostatSignal> signal;

        @Override
        public void consume(DataSample<ThermostatSignal> signal) {

            this.signal = signal;

            refresh();
        }

        public void refresh() {

            DataSample<Double> pv = source.getController().getProcessVariable();
            ThermostatSignal sample = signal == null ? null : signal.sample; 

            String displayTemperature;

            if (pv == null) {

                displayTemperature = UNDEFINED;

            } else {

                double currentTemperature = pv.sample;

                currentTemperature = needFahrenheit ? (currentTemperature * 9) / 5 + 32: currentTemperature;
                displayTemperature = (sample == null) ? UNDEFINED : (sample.demand.isError() ? UNDEFINED : new Formatter().format(Locale.getDefault(), "%.1f", currentTemperature).toString());

                TintedValue v = new TintedValue(currentTemperature, source.getControlSignal() * 2, sample.calling);
                chart.consume(new DataSample<TintedValue>(pidListener.signal.timestamp, "temp", "temp", v, null));

                //logger.debug("VALUE: " + v);
            }

            Color fg = ColorScheme.getScheme(getMode()).SETPOINT;
            Font font = needFahrenheit  && displayTemperature.length() > 4 ? currentFontF : currentFontC;

            currentLabel.setFont(font);
            currentLabel.setText(displayTemperature);
            currentLabel.setForeground(fg);
            setpointLabel.setForeground(fg);

            votingLabel.setText(source.isVoting() ? VOTING : NOT_VOTING);
            holdLabel.setText(source.isOnHold() ? ON_HOLD : HOLD);

            votingLabel.setForeground(source.isVoting() ? ColorScheme.getScheme(getMode()).NOTICE_DEFAULT : ColorScheme.getScheme(getMode()).NOTICE_ACTIVE);
            holdLabel.setForeground(source.isOnHold() ? ColorScheme.getScheme(getMode()).NOTICE_ACTIVE : ColorScheme.getScheme(getMode()).NOTICE_DEFAULT);

            renderPeriod();

            repaint();
        }

        public void renderPeriod() {

            if (scheduler == null) {

                periodLabel.setText(NO_PERIOD);
                return;
            }

            Period p = scheduler.getCurrentPeriod(source);

            if (p == null) {

                periodLabel.setText(NO_PERIOD);
                return;
            }

            StringBuilder sb = new StringBuilder(); 

            ZoneStatus status = scheduler.getCurrentStatus(source);

            if (status == null) {

                logger.warn("Period is resolved as " + p + ", but no status???");

            } else {

                if (!status.equals(source.getStatus())) {

                    sb.append("* ");
                }
            }

            sb.append(p.name);

            periodLabel.setText(sb.toString());
        }
    }

    private class PidControllerListener implements DataSink<ProcessControllerStatus> {

        public DataSample<PidControllerStatus> signal;

        @Override
        public void consume(DataSample<ProcessControllerStatus> signal) {

            PidControllerStatus sample = (PidControllerStatus) signal.sample;

            this.signal = new DataSample<PidControllerStatus>(signal.timestamp, signal.sourceName, signal.signature, sample, signal.error);

            refresh();
        }

        public void refresh() {

            String label;

            if (this.signal == null) {

                label = UNDEFINED + "\u00b0";

            } else {

                double setpoint = this.signal.sample.setpoint;

                setpoint = needFahrenheit ? (setpoint * 9) / 5 + 32: setpoint;

                label = source.isOn() ? new Formatter().format(Locale.getDefault(), "%.1f\u00b0", setpoint).toString() : "OFF";
            }

            Color fg = ColorScheme.getScheme(getMode()).SETPOINT;

            setpointLabel.setText(label);
            setpointLabel.setForeground(fg);

            // The signal path is such that this code will always be executed right before ThermostatListener#refresh
            // which also calls repaint(), hence this call is redundant.

            // repaint();
        }
    }

    @Override
    public synchronized void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (pidListener.signal == null || thermostatListener.signal == null) {

            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        Dimension d = getSize();

        Double signal = thermostatListener.signal.sample.demand.sample;
        HvacMode mode = getMode();
        ZoneState state = thermostatListener.signal == null ? null : (thermostatListener.signal.sample.calling ? ZoneState.CALLING : ZoneState.HAPPY);

        if ( thermostatListener.signal == null || thermostatListener.signal.sample.demand.isError()) {

            state = ZoneState.ERROR;

        } else if (!thermostatListener.signal.sample.enabled) {

            state = ZoneState.OFF;
        }

        Rectangle boundary = new Rectangle(0, 0, d.width, d.height);

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

    public void setFontSize(ScreenDescriptor screenDescriptor) {

        this.currentFontC = screenDescriptor.fontCurrentTemperatureC;
        this.currentFontF = screenDescriptor.fontCurrentTemperatureF;
        this.setpointFont = screenDescriptor.fontSetpoint;
    }

    @Override
    public Dimension getPreferredSize() {

        Dimension d = super.getPreferredSize();

        //		logger.debug("PREFERRED SIZE: " + d);

        return d;
    }
}
