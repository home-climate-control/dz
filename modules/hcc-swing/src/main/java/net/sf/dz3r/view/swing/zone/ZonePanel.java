package net.sf.dz3r.view.swing.zone;

import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.model.PeriodSettings;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.runtime.config.model.TemperatureUnit;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import net.sf.dz3r.view.swing.ColorScheme;
import net.sf.dz3r.view.swing.EntityPanel;
import net.sf.dz3r.view.swing.EntitySelectorPanel;
import net.sf.dz3r.view.swing.ScreenDescriptor;
import reactor.core.publisher.Flux;

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
import java.util.Optional;

import static java.awt.event.KeyEvent.KEY_PRESSED;

/**
 * Zone panel.
 *
 * Even though it implements {@link KeyListener}, it never request focus,
 * but gets event notifications from {@link EntitySelectorPanel} instead.
 * This is done in order not to fiddle with focus changes.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ZonePanel extends EntityPanel<ZoneStatus, Void> {

    private static final DecimalFormat numberFormat = new DecimalFormat("#0.0;-#0.0");

    private static final String UNDEFINED = "--.-";

    private static final String VOTING = "VOTING";
    private static final String NOT_VOTING = "NOT VOTING";

    private static final String HOLD = "HOLD";
    private static final String ON_HOLD = "ON HOLD";

    private static final String NO_PERIOD = "(no period is active)";

    /**
     * Setpoint change upon a keypress.
     */
    private static final double SETPOINT_DELTA = 0.1d;

    private final JLabel currentLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);
    private final JLabel setpointLabel = new JLabel(UNDEFINED + "°", SwingConstants.RIGHT);
    private final JLabel votingLabel = new JLabel(VOTING, SwingConstants.RIGHT);
    private final JLabel holdLabel = new JLabel(HOLD, SwingConstants.RIGHT);
    private final JLabel periodLabel = new JLabel("", SwingConstants.LEFT);

    /**
     * Font to display the current temperature in Celsius.
     */
    private Font currentFontC;

    /**
     * Font to display the current temperature in Fahrenheit when the temperature is over 100F.
     *
     * @see #needFahrenheit
     */
    private Font currentFontF;

    /**
     * Font to display the setpoint with.
     */
    private Font setpointFont;

    private boolean needFahrenheit;

    private final transient Zone zone;

    private final AbstractZoneChart chart;

    private transient ZoneStatus zoneStatus;

    /**
     * @see #consumeSensorSignal(Signal)
     */
    private transient Signal<Double, Void> sensorSignal;

    /**
     * @see #consumeMode(Signal)
     */
    private HvacMode hvacMode;

    /**
     * Create an instance.
     *
     * @param zone Zone to display the status of.
     * @param defaultUnit Temperature unit at instantiation time.
     */
    public ZonePanel(Zone zone, ScreenDescriptor screenDescriptor, TemperatureUnit defaultUnit,
                     Flux<Signal<ZoneStatus, Void>> zoneFlux,
                     Flux<Signal<Double, Void>> sensorFlux,
                     Flux<Signal<HvacMode, Void>> modeFlux) {

        this.zone = zone;

        needFahrenheit = defaultUnit == TemperatureUnit.F;
        chart = new ZoneChart2021(Clock.systemUTC(), 1000L * 60 * 60 * 3, needFahrenheit);

        setFontSize(screenDescriptor);

        initGraphics();

        zoneFlux.subscribe(this::consumeSignal);
        sensorFlux.subscribe(this::consumeSensorSignal);
        modeFlux.subscribe(this::consumeMode);

        initKeyStream();
    }

    private void initGraphics() {

        currentLabel.setFont(currentFontC);
        currentLabel.setToolTipText("<html>Current temperature<hr>Left/Right to change zone</html>");

        setpointLabel.setFont(setpointFont);
        setpointLabel.setToolTipText("<html>Setpoint<hr>"
                + "Up/Down to change by " + numberFormat.format(SETPOINT_DELTA) + "&deg;<br>"
                + "Shift+Up/Down to change by " + numberFormat.format(SETPOINT_DELTA * getSetpointDeltaModifier(true, false)) + "&deg;<br>"
                + "Ctrl+Up/Down to change by " + numberFormat.format(SETPOINT_DELTA * getSetpointDeltaModifier(false, true)) + "&deg;<br>"
                + "Shift+Ctrl+Up/Down to change by " + numberFormat.format(SETPOINT_DELTA * getSetpointDeltaModifier(true, true)) + "&deg;<br>"
                + "</html>");

        createLayout(zone.getAddress(), chart);
    }

    @Override
    @SuppressWarnings("squid:S1199")
    protected void createControls(JPanel controls, GridBagLayout layout, GridBagConstraints cs) {

        // VT: NOTE: squid:S1199 - SonarLint is not smart enough to realize that these
        // blocks are for readability

        {
            // Period label is on top left

            cs.gridwidth = 2;
            cs.fill = GridBagConstraints.HORIZONTAL;

            layout.setConstraints(periodLabel, cs);
            controls.add(periodLabel);

            periodLabel.setForeground(Color.GRAY);

            // No text is set to distinguish the situation when the zone schedule is not yet initialized
            // from "no period is active"
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
    }

    @Override
    public void setFontSize(ScreenDescriptor screenDescriptor) {
        this.currentFontC = screenDescriptor.fontCurrentTemperatureC;
        this.currentFontF = screenDescriptor.fontCurrentTemperatureF;
        this.setpointFont = screenDescriptor.fontSetpoint;
    }

    @Override
    protected boolean isBackgroundTransparent() {
        return true;
    }


    private void initKeyStream() {
        keyFlux
                .flux
                .filter(e -> e.getID() == KEY_PRESSED)
                .flatMap(this::consumeKeypress)
                .subscribe(s -> logger.info("{}: {}", zone.getAddress(), s));
    }


    private Flux<String> consumeKeypress(KeyEvent e) {

        logger.debug("{}", e::toString);

        if (zoneStatus == null) {
            return Flux.just("zoneStatus unset, blowups likely, ignored: " + e);
        }

        switch (Character.toLowerCase(e.getKeyChar())) {
            case 'c', 'f' -> {
                needFahrenheit = !needFahrenheit;
                refresh();
                return Flux.just("displaying temperature in " + (needFahrenheit ? "Fahrenheit" : "Celsius"));
            }
            case 'h' -> {

                // Toggle hold status

                zone.setSettingsSync(zone.getSettings().merge(new ZoneSettings(
                        null,
                        null,
                        null,
                        !zoneStatus.settings.hold,
                        null
                )));
                refresh();
                return Flux.just("hold status is now " + zone.getSettings().hold);
            }
            case 'v' -> {

                // Toggle voting status

                zone.setSettingsSync(zone.getSettings().merge(new ZoneSettings(
                        null,
                        null,
                        !zoneStatus.settings.voting,
                        null,
                        null
                )));
                refresh();
                return Flux.just("voting status is now " + zone.getSettings().voting);
            }
            case 'o' -> {

                // Toggle off status

                zone.setSettingsSync(new ZoneSettings(zone.getSettings(), !zoneStatus.settings.enabled));
                refresh();
                return Flux.just("on status is now " + zone.getSettings().enabled);
            }
            case 's' -> {

                // Go back to schedule

                // Implies taking the zone off hold
                zone.setSettingsSync(zone.getSettings().merge(new ZoneSettings(
                        null,
                        null,
                        null,
                        false,
                        null
                )));
                activateSchedule();
                refresh();
            }
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {

                // Change dump priority

                zone.setSettingsSync(zone.getSettings().merge(new ZoneSettings(
                        null,
                        null,
                        null,
                        null,
                        e.getKeyChar() - '0'
                )));
                refresh();
                return Flux.just("dump priority is now " + zone.getSettings().dumpPriority);
            }
            case KeyEvent.CHAR_UNDEFINED -> {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_KP_UP, KeyEvent.VK_UP -> {
                        return raiseSetpoint(getSetpointDeltaModifier(e.isShiftDown(), e.isControlDown()));
                    }
                    case KeyEvent.VK_KP_DOWN, KeyEvent.VK_DOWN -> {
                        return lowerSetpoint(getSetpointDeltaModifier(e.isShiftDown(), e.isControlDown()));
                    }
                    default -> {
                        // Do nothing
                    }
                }
            }
            default -> {
                // Do nothing
            }
        }

        return Flux.empty();
    }

    private void activateSchedule() {

        if (zoneStatus.periodSettings == null) {
            logger.error("{}: activateSchedule(): no period active", zone.getAddress());
            return;
        }

        var settings = zoneStatus.periodSettings.settings();

        logger.info("{}: returning to settings: {}", zone.getAddress(), settings);

        zone.setSettingsSync(settings);
    }

    private void refresh() {
        logger.warn("{}: refresh(): NOP?", zone.getAddress());
    }

    private int getSetpointDeltaModifier(boolean shift, boolean ctrl) {

        if (shift && ctrl) {
            return 100;
        }

        if (shift) {
            return 10;
        }

        if (ctrl) {
            return 50;
        }

        return 1;
    }

    /**
     * Change the setpoint.
     *
     * @param delta Amount to change by.
     */
    private Flux<String> changeSetpoint(double delta) {

        try {

            // Must operate in visible values to avoid rounding problems
            var setpoint = getDisplayValue(zone.getSettings().setpoint);

            setpoint += delta;
            setpoint = getSIValue(Double.parseDouble(numberFormat.format(setpoint)));

            final var s = setpoint;

            // This may blow up if the zone refuses to take the new setpoint because it is out of range
            return Flux.just(1)
                    .flatMap(ignore -> zone.setSettings(new ZoneSettings(zone.getSettings(), s)))
                    .doOnNext(signal -> {
                        if (!signal.isOK()) {
                            logger.error("{}: changeSetpoint({}) failed", zone.getAddress(), delta, signal.error);
                        }
                    })
                    .filter(Signal::isOK)
                    .map(Signal::getValue)
                    .map(Object::toString);

        } finally {
            refresh();
        }
    }

    /**
     * Raise the setpoint.
     *
     * @param modifier Multiply the default {@link #SETPOINT_DELTA} by this number to get the actual delta.
     */
    private Flux<String> raiseSetpoint(int modifier) {
        return changeSetpoint(SETPOINT_DELTA * modifier);
    }

    /**
     * Lower the setpoint.
     *
     * @param modifier Multiply the default {@link #SETPOINT_DELTA} by this number to get the actual delta.
     */
    private Flux<String> lowerSetpoint(int modifier) {
        return changeSetpoint(-SETPOINT_DELTA * modifier);
    }

    /**
     * Process {@link ZoneStatus} updates (see {@link net.sf.dz3r.view.swing.SwingSink}).
     */
    @Override
    protected boolean update(Signal<ZoneStatus, Void> signal) {

        this.zoneStatus = signal.getValue();

        if (!signal.isOK()) {
            logger.error("{}: not implemented: processing !OK signal: {}", zone.getAddress(), signal, new UnsupportedOperationException());
            return false;
        }

        var setpoint = Optional.ofNullable(zoneStatus.settings.setpoint);
        var voting = Optional.ofNullable(zoneStatus.settings.voting);
        var hold = Optional.ofNullable(zoneStatus.settings.hold);

        setpoint.ifPresent(s -> setpointLabel.setText(String.format(Locale.getDefault(), "%.1f°", getDisplayValue(s))));

        voting.ifPresent(v -> {
            votingLabel.setText(Boolean.TRUE.equals(v) ? VOTING : NOT_VOTING);
            votingLabel.setForeground(Boolean.TRUE.equals(v) ? ColorScheme.getScheme(getMode()).noticeDefault : ColorScheme.getScheme(getMode()).noticeActive);
        });

        hold.ifPresent(h -> {
            holdLabel.setText(Boolean.TRUE.equals(h) ? ON_HOLD : HOLD);
            holdLabel.setForeground(Boolean.TRUE.equals(h) ? ColorScheme.getScheme(getMode()).noticeActive : ColorScheme.getScheme(getMode()).noticeDefault);
        });

        renderPeriod();

        return true;
    }

    public void consumeSensorSignal(Signal<Double, Void> sensorSignal) {
        this.sensorSignal = sensorSignal;
        logger.debug("{}: sensorSignal: {}", zone.getAddress(), sensorSignal);
        updateSensorSignal();
    }

    /**
     * Selectively update only the UI parts affected by a changed sensor signal.
     *
     * We do not process error signal here, it will propagate to {@link #update(Signal)}}.
     */
    private void updateSensorSignal() {

        var displayTemperature = Optional.ofNullable(sensorSignal.getValue())
                .map(v -> String.format(Locale.getDefault(), "%.1f", getDisplayValue(v)))
                .orElse(UNDEFINED);
        currentLabel.setText(displayTemperature);

        var font = needFahrenheit  && displayTemperature.length() > 4 ? currentFontF : currentFontC;
        currentLabel.setFont(font);

        updateChart();
        repaint();
    }

    private void updateChart() {

        // Current temperature is guaranteed to be available at this point, but control signal may not be

        if (zoneStatus == null) {
            logger.warn("{}: zoneStatus null, not updating the chart", zone.getAddress());
            return;
        }

        try {

            var dataPoint = new ZoneChartDataPoint(
                    new ThermostatTintedValue(
                            sensorSignal.getValue(),
                            zoneStatus.callingStatus.demand * 2,
                            zoneStatus.callingStatus.calling),
                    zoneStatus.settings.setpoint,
                    zoneStatus.economizerStatus);

            // VT: FIXME: This must be driven via Flux
            chart.consumeSignal(new Signal<>(getSignal().timestamp, dataPoint));
        } catch (Exception ex) {
            logger.error("unexpected exception processing {}", sensorSignal, ex);
        }
    }

    private HvacMode getMode() {
        return hvacMode;
    }

    /**
     * Selectively update only the UI parts affected by a changed HVAC mode.
     */
    public void consumeMode(Signal<HvacMode, Void> hvacModeSignal) {

        var hvacMode = hvacModeSignal.getValue(); // NOSONAR I know

        if (this.hvacMode == hvacMode) {
            return;
        }

        this.hvacMode = hvacMode;
        logger.debug("{}: hvacMode: {}", zone.getAddress(), hvacMode);

        // The way the lifecycle is built, the only updates are the setpoint and current temperature colors,
        // and only from "unknown" to "mode specific".

        var c = ColorScheme.getScheme(getMode()).setpoint;
        currentLabel.setForeground(c);
        setpointLabel.setForeground(c);
    }

    private void renderPeriod() {

        if (zoneStatus == null) {
            logger.warn("{} renderPeriod(): no zone status yet", zone.getAddress());
            return;
        }

        if (zoneStatus.periodSettings == null) {
            periodLabel.setText(NO_PERIOD);
        } else {
            periodLabel.setText(zoneStatus.periodSettings.period().name + (isOnSchedule() ? "" : "*"));
        }
    }

    /**
     * @return {@code true} if the zone settings are identical to those of the current schedule period, {@code false} otherwise.
     */
    private boolean isOnSchedule() {
        return zoneStatus.settings.same(
                Optional.ofNullable(zoneStatus.periodSettings)
                        .map(PeriodSettings::settings)
                        .orElse(null));
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

    @Override
    public synchronized void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (zoneStatus == null || sensorSignal == null) {
            // Nothing to do yet
            return;
        }

        var g2d = (Graphics2D) g;
        var d = getSize();

        var signal = zoneStatus.callingStatus.demand;
        var mode = getMode();
        var state = resolveState();
        var boundary = new Rectangle(0, 0, d.width, d.height);

        switch (state) {
            case CALLING, ERROR, OFF -> BackgroundRenderer.drawBottom(state, mode, signal, g2d, boundary, true);
            case HAPPY -> BackgroundRenderer.drawTop(mode, signal, g2d, boundary);
        }
    }

    private Zone.State resolveState() {

        if (getSignal() == null || getSignal().isError()) {
            return Zone.State.ERROR;
        }

        if (Boolean.FALSE.equals(zoneStatus.settings.enabled)) {
            return Zone.State.OFF;
        }

        return zoneStatus.callingStatus.calling ? Zone.State.CALLING : Zone.State.HAPPY;
    }
}
