package net.sf.dz3.view.swing.unit;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import net.sf.dz3.view.swing.ColorScheme;
import net.sf.dz3.view.swing.EntityPanel;
import net.sf.dz3.view.swing.ScreenDescriptor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;

public class UnitPanel extends EntityPanel {

    private static final String UNDEFINED = "--";

    private final JLabel demandLabel = new JLabel("Demand", SwingConstants.LEFT);
    private final JLabel currentDemandLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final JLabel runningForLabel = new JLabel("Running for", SwingConstants.LEFT);
    private final JLabel currentRunningForLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final JLabel leftLabel = new JLabel("Time left", SwingConstants.LEFT);
    private final JLabel currentLeftLabel = new JLabel(UNDEFINED, SwingConstants.RIGHT);

    private final UnitChart unitChart = new UnitChart();

    private final Listener listener = new Listener();

    /**
     * Create an instance.
     *
     * @param source Data source.
     */
    public UnitPanel(RuntimePredictor source) {

        initGraphics();
        source.addConsumer(unitChart);
    }

    @SuppressWarnings("squid:S1199")
    private void initGraphics() {
        // VT: NOTE: Introducing the name is not the priority at the moment - let's steal it from the signal
        // when it comes
        createLayout("--", unitChart);
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
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 2;
            cs.fill = GridBagConstraints.HORIZONTAL;

            layout.setConstraints(demandLabel, cs);
            controls.add(demandLabel);

            demandLabel.setForeground(Color.GRAY);
        }

        return controls;
    }

    @Override
    public void setFontSize(ScreenDescriptor screenDescriptor) {
        // No special handling yet
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No special handling
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // No special handling yet
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No special handling
    }

    private class Listener  implements DataSink<UnitRuntimePredictionSignal> {

        @Override
        public void consume(DataSample<UnitRuntimePredictionSignal> signal) {
            ((TitledBorder) getBorder()).setTitle(signal.sourceName);
            unitChart.consume(signal);
        }
    }
}
